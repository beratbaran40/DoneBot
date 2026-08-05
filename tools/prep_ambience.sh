#!/usr/bin/env bash
#
# Prepares Pomodoro ambience loops for res/raw. Three things happen here.
#
#  1. Seam matching. A clip cut at an arbitrary point restarts with an audible jump. We
#     fade the last SEAM seconds out, mix them over the first SEAM seconds faded in, and
#     move that crossfade to the FRONT of the file — so the file's first sample follows
#     naturally from its last one and MediaPlayer's isLooping restart is inaudible.
#
#  2. Loudness matching. The raw sources sit 18 dB apart (rain -36 LUFS, handpan -18),
#     which would make every ambience switch a volume-knob emergency. Each output is
#     brought to TARGET_LUFS with a single constant gain — constant on purpose, because a
#     time-varying one (what loudnorm's dynamic mode applies) would re-break the seam the
#     first step just built.
#
#  3. Re-encoding to Ogg Opus. MP3 carries encoder delay/padding that shows up as a gap on
#     every loop, and 128 kbps stereo is wasteful for an ambience bed. Opus stores its
#     pre-skip in the container so the decoder trims it exactly; noise-like beds go mono,
#     melodic ones keep their stereo image. Android decodes Ogg Opus natively since API 21
#     (minSdk here is 26).
#
# Usage:  tools/prep_ambience.sh
# Requires ffmpeg + ffprobe (brew install ffmpeg).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT/pomodoro_background_noises"
OUT_DIR="$ROOT/app/src/main/res/raw"

SEAM=2          # seconds of tail crossfaded over the head
TARGET_LUFS=-23 # EBU R128; a background bed under a 96sp timer, not a foreground track
PEAK_CEILING=-1 # dBFS, never exceeded

command -v ffmpeg >/dev/null || { echo "ffmpeg not found — brew install ffmpeg" >&2; exit 1; }
command -v ffprobe >/dev/null || { echo "ffprobe not found — brew install ffmpeg" >&2; exit 1; }

mkdir -p "$OUT_DIR"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

measure_lufs() { # <file> -> integrated loudness in LUFS
  ffmpeg -hide_banner -nostats -i "$1" -af ebur128=framelog=quiet -f null - 2>&1 |
    grep -A1 "Integrated loudness" | grep -o 'I: *[-0-9.]*' | awk '{print $2}'
}

measure_peak() { # <file> -> max sample peak in dBFS
  ffmpeg -hide_banner -nostats -i "$1" -af volumedetect -f null - 2>&1 |
    grep -o 'max_volume: [-0-9.]*' | awk '{print $2}'
}

# prepare <source file> <output basename> <channels: 1|2> <opus bitrate>
prepare() {
  local src="$1" name="$2" channels="$3" bitrate="$4"
  local out="$OUT_DIR/$name.ogg"
  local wav="$TMP_DIR/$name.wav"

  [ -f "$src" ] || { echo "missing source: $src" >&2; exit 1; }

  local dur tail_start body_end
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$src")
  tail_start=$(awk "BEGIN { printf \"%.6f\", $dur - $SEAM }")
  body_end=$tail_start

  # Step 1 — seam, into a lossless intermediate so the measurement below sees final material.
  #
  #   [head] source[0 .. SEAM]         faded in
  #   [tail] source[dur-SEAM .. dur]   faded out
  #   [seam] = head + tail             (SEAM long)
  #   [body] source[SEAM .. dur-SEAM]
  #   output = seam ++ body            (dur-SEAM long)
  #
  # Loop point: output ends on source[dur-SEAM-e] and restarts on seam[0], which is exactly
  # source[dur-SEAM] — consecutive samples of the original, so nothing jumps. The internal
  # join is continuous for the same reason: seam ends on source[SEAM-e], body starts on
  # source[SEAM]. qsin is the equal-power fade curve; the default triangular one dips ~3 dB
  # mid-crossfade, which reads as a dropout on a steady noise bed.
  ffmpeg -hide_banner -loglevel error -y -i "$src" -filter_complex "\
[0:a]atrim=start=0:end=$SEAM,asetpts=N/SR/TB,afade=t=in:st=0:d=$SEAM:curve=qsin[head];\
[0:a]atrim=start=$tail_start,asetpts=N/SR/TB,afade=t=out:st=0:d=$SEAM:curve=qsin[tail];\
[head][tail]amix=inputs=2:duration=shortest:normalize=0[seam];\
[0:a]atrim=start=$SEAM:end=$body_end,asetpts=N/SR/TB[body];\
[seam][body]concat=n=2:v=0:a=1[out]" \
    -map "[out]" -ac "$channels" -ar 48000 -c:a pcm_s24le "$wav"

  # Step 2 — one constant gain toward the target.
  #
  # Peaky material (fire crackles) wants more gain than its headroom allows. The fix must be
  # MEMORYLESS or it undoes step 1: a limiter's envelope follower applies different gain at
  # the file's end than at its start, so the two samples that met at the loop point no longer
  # line up. Measured on this exact material, alimiter blew the loop discontinuity up to 84x
  # the typical sample step — an audible click every cycle. asoftclip is a static waveshaper,
  # so the same input sample always maps to the same output sample no matter where it sits;
  # it hit the same -23 LUFS with the loop point still continuous (0.6x).
  local lufs peak gain headroom shaped=""
  lufs=$(measure_lufs "$wav")
  peak=$(measure_peak "$wav")
  gain=$(awk "BEGIN { printf \"%.2f\", $TARGET_LUFS - ($lufs) }")
  headroom=$(awk "BEGIN { printf \"%.2f\", $PEAK_CEILING - ($peak) }")
  if awk "BEGIN { exit !($gain > $headroom) }"; then
    shaped=",asoftclip=type=tanh:threshold=$(awk "BEGIN { printf \"%.4f\", 10 ^ ($PEAK_CEILING / 20) }")"
  fi

  # Step 3 — encode.
  ffmpeg -hide_banner -loglevel error -y -i "$wav" \
    -af "volume=${gain}dB${shaped}" \
    -ac "$channels" -ar 48000 -c:a libopus -b:a "$bitrate" -application audio \
    -map_metadata -1 "$out"

  local bytes out_lufs out_peak out_dur
  bytes=$(stat -f%z "$out")
  out_dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$out")
  out_lufs=$(measure_lufs "$out")
  out_peak=$(measure_peak "$out")
  printf '%-24s %6s KB  %6.2fs  %dch %-4s  %+.1f dB -> %6s LUFS  peak %6s dBFS%s\n' \
    "$name.ogg" "$((bytes / 1024))" "$out_dur" "$channels" "$bitrate" \
    "$gain" "$out_lufs" "$out_peak" "${shaped:+  (soft-clipped)}"
}

echo "Preparing ambience loops → $OUT_DIR   (target ${TARGET_LUFS} LUFS, ceiling ${PEAK_CEILING} dBFS)"
# Noise-like beds: mono is indistinguishable and halves the bytes.
prepare "$SRC_DIR/rain.MP3"      ambience_rain      1 56k
prepare "$SRC_DIR/fireplace.MP3" ambience_fireplace 1 56k
# Melodic: keep the stereo image, spend a little more bitrate on it.
prepare "$SRC_DIR/handpan.MP3"   ambience_handpan   2 96k

total=$(find "$OUT_DIR" -name 'ambience_*.ogg' -exec stat -f%z {} + | awk '{s+=$1} END {print s}')
printf '\ntotal %s KB against the 20 MiB AAB budget\n' "$((total / 1024))"
