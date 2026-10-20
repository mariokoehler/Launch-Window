"""Publishes a batch of replay clips, several at a time: python publish.py [-j N] [--dry-run] <clip> ...

A clip is a step clip's name (steps/build.py's STEPS), or launch:<skin> for a craft's launch (launch-storyboard/build.py's
SKINS; its three variants). Each runs its builder with "publish nocues", logged to steps/build/logs/<clip>.log; the
sound cues go into the sidecars once, after every clip has rendered (cues.py), so parallel publishes never write the
same sidecar. One EffectCraft render keeps about three of this PC's cores busy (user, 2026-10-20): -j 3 by default.
"""
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
LOGS = os.path.join(HERE, "steps", "build", "logs")


def jobs(clips):
    """(log name, working folder, command) for each clip; a launch is three, one a variant."""
    out = []
    for clip in clips:
        if clip.startswith("launch:"):
            skin = clip.split(":", 1)[1]
            for v in ("success", "glitch", "failure"):
                out.append((f"launch-{skin}-{v}", os.path.join(HERE, "launch-storyboard"),
                            [sys.executable, "build.py", v, skin, "publish", "nocues"]))
        else:
            out.append((clip, os.path.join(HERE, "steps"), [sys.executable, "build.py", clip, "publish", "nocues"]))
    return out


def run(job):
    name, cwd, cmd = job
    start = time.time()
    with open(os.path.join(LOGS, name + ".log"), "w", encoding="utf-8") as log:
        code = subprocess.run(cmd, cwd=cwd, stdout=log, stderr=subprocess.STDOUT).returncode
    return name, code, time.time() - start


def main():
    args = sys.argv[1:]
    n = 3
    if "-j" in args:
        i = args.index("-j")
        n = int(args[i + 1])
        del args[i:i + 2]
    dry = "--dry-run" in args
    clips = [a for a in args if a != "--dry-run"]
    todo = jobs(clips)
    if dry:
        for name, cwd, cmd in todo:
            print(f"{name}: {os.path.relpath(cwd, HERE)}> {' '.join(cmd[1:])}")
        return
    os.makedirs(LOGS, exist_ok=True)
    start, failed = time.time(), []
    with ThreadPoolExecutor(max_workers=n) as pool:
        for name, code, secs in pool.map(run, todo):
            print(f"{'ok  ' if code == 0 else 'FAIL'} {name} ({secs:.0f} s)", flush=True)
            if code:
                failed.append(name)
    for name in failed:
        print(f"--- {name}:")
        print("".join(open(os.path.join(LOGS, name + ".log"), encoding="utf-8").readlines()[-8:]))
    sys.path.insert(0, HERE)
    import cues
    cues.apply()
    print(f"{len(todo) - len(failed)} of {len(todo)} published in {time.time() - start:.0f} s, {n} at a time; cues applied")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
