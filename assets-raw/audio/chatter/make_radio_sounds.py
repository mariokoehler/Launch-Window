"""Publishes the radio call-outs: python assets-raw/audio/chatter/make_radio_sounds.py

Copies the takes in this folder (made with VibeVoice in ComfyUI, the user's workflow; the user's own, or
Claude's from 2026-10-15, screened with Whisper and picked by the user's ear; each file's name is what's
said, "(n)" a variation of one line) into assets/audio/replay/ as radio-<id>[-n].mp3, one sound per
meaning, and writes their entries into assets/content/replay-sounds.json (any radio-* entries there are
replaced). Where they play is in assets-raw/replay/cues.json. A new take: add its file name to its sound
below, rerun, and list it in assets/CREDITS.md's radio bullet.
"""
import json
import os
import shutil

SRC = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(SRC, "..", "..", ".."))
DST = os.path.join(REPO, "assets", "audio", "replay")
CATALOG = os.path.join(REPO, "assets", "content", "replay-sounds.json")


def takes(line, n):
    """A line's takes, "<line> (1)" to "<line> (n)"."""
    return [f"{line} ({i})" for i in range(1, n + 1)]


# id: (source files, what's said and where it plays)
SOUNDS = {
    "radio-go-for-launch": (["go_for_launch", "all_systems_go_for_launch"],
        "\"Go for launch.\" / \"All systems go for launch.\" The launch preparations' success, at its reveal."),
    "radio-guidance-internal": (["guidance_is_internal"],
        "\"Guidance is internal.\" Early in the countdown, in every variant (it tells nothing)."),
    "radio-pitching-downrange": (["vehicle_pitching_downrange", "vehicle_is_supersonic"],
        "\"Vehicle pitching downrange.\" / \"Vehicle is supersonic.\" The launch's success, at its reveal."),
    "radio-max-q": (["vehicle_passing_through_max_q"],
        "\"Vehicle passing through Max-Q.\" The ascent's opening, in every variant (it tells nothing)."),
    "radio-staging": (["stage_separation_confirmed", "second_stage_on_nominal_trajectory",
                       "fairing_separation_confirmed"],
        "\"Stage separation confirmed.\" / \"Second stage on nominal trajectory.\" / \"Fairing separation "
        "confirmed.\" The ascent's success, after the Max-Q call."),
    "radio-orbit-insertion": (["nominal_orbit_insertion"],
        "\"Nominal orbit insertion.\" The orbit insertion's success, at its reveal."),
    "radio-payload-separation": (["payload_separation_confirmed"],
        "\"Payload separation confirmed.\" The deployment's success, at its reveal."),
    "radio-signal-good": (["power_and_telemetry_nominal", "data_coming_in_now (1)", "data_coming_in_now (2)",
                           "data_coming_in_now (3)"],
        "\"Power and telemetry nominal.\" / \"Data coming in now.\" A satellite in orbit calling home, its "
        "success, at the reveal; the carrier entry's success (its data home before it burns up); the "
        "landers' descents at Venus and Mars, their successes."),
    "radio-watching": (["watching_it", "we_are_looking_at_it", "standby"],
        "\"Watching it.\" / \"We're looking at it.\" / \"Stand by.\" Before a reveal, in every variant: the "
        "deployment's opening."),
    "radio-countdown-hold": (["copy_standing_by"] + takes("hold_hold_hold_recycling_counting_again", 3),
        "\"Copy, standing by.\" / \"Hold, hold, hold. … Recycling. Counting again.\" The countdown's hold (its "
        "glitch), at the reveal."),
    "radio-roger": (["roger_copy (1)", "roger_copy (2)"],
        "\"Roger, copy.\" An acknowledgement: the reentry preparations' success."),
    "radio-engine-out": (["flight_booster_we_have_an_engine_out_remaining_engines_are_compensating",
                          "inboard_engine_out_we_are_still_go (1)", "inboard_engine_out_we_are_still_go (2)"],
        "\"Flight, Booster. We have an engine out. Remaining engines are compensating.\" / \"Inboard engine "
        "out. We're still go.\" Not placed yet: no clip shows an engine out on a many-engined stage."),
    "radio-second-stage-late": (["flight_booster_second_stage_ignition_late_but_it_is_good_thrust_now (1)",
                                 "flight_booster_second_stage_ignition_late_but_it_is_good_thrust_now (2)"],
        "\"Flight, Booster. Second stage ignition late, but it's good thrust now.\" Not placed yet: no clip "
        "shows a late ignition."),
    "radio-trajectory-low": (["trajectory_is_a_little_low_guidance_is_correcting (1)",
                              "trajectory_is_a_little_low_guidance_is_correcting (2)"],
        "\"Trajectory is a little low. Guidance is correcting.\" The ascent's glitch (the upper stage swings "
        "off its path and is steered back)."),
    "radio-oscillations": (["we_are_seeing_some_oscillations_within_limits"],
        "\"We're seeing some oscillations. Within limits.\" The reentry's glitch, and the landers' entries' "
        "at Venus and Mars; Venera's descent's (its chute torn, swinging)."),
    "radio-telemetry-lost": (["flight_we_have_lost_telemetry_we_have_it_back_signal_is_good",
                              "loss_of_signal_acquisition_of_signal_data_looks_good (1)",
                              "loss_of_signal_acquisition_of_signal_data_looks_good (2)"],
        "\"Flight, we've lost telemetry. … We have it back. Signal is good.\" / \"Loss of signal. … "
        "Acquisition of signal. Data looks good.\" A satellite's or a crew's signal dropping out and coming "
        "back: the glitches of the Earth orbit, the biometric data and the carrier entry."),
    "radio-undervolt": (["flight_main_bus_undervolt_switching_to_backup_voltage_is_coming_back (1)",
                         "flight_main_bus_undervolt_switching_to_backup_voltage_is_coming_back (2)",
                         "flight_main_bus_undervolt_switching_to_backup_voltage_is_coming_back (3)"]
                        + takes("try_sce_to_aux", 2),
        "\"Flight, main bus undervolt. Switching to backup. Voltage is coming back.\" / \"Try SCE to Aux.\" "
        "(Apollo 12.) The power dropping and the backup coming in: the experiments' and the long flights' "
        "glitches."),
    "radio-attitude-drift": (["attitude_control_is_drifting_switching_to_backup_rates_are_damping"],
        "\"Attitude control is drifting. Switching to backup. Rates are damping.\" A turn stopped late: the "
        "suborbital flight's and the reentry preparations' glitches; the Mars lander's descent's (an engine "
        "late, the lander tipping)."),
    "radio-burn-short": (["flight_the_burn_was_short_we_can_trim_it_on_the_next_pass"],
        "\"Flight, the burn was short. We can trim it on the next pass.\" The orbit insertion's glitch (the "
        "burn cuts out and relights)."),
    "radio-separation-late": (["separation_indicator_did_not_light_confirmed_by_telemetry_we_have_separation (1)",
                               "separation_indicator_did_not_light_confirmed_by_telemetry_we_have_separation (2)"],
        "\"Separation indicator did not light. … Confirmed by telemetry. We have separation.\" Something that "
        "should let go and doesn't at first: the deployment's, the retropack's and the modules' glitches."),
    "radio-chute-partial": (["one_chute_is_not_fully_inflated_two_good_mains_landing_should_be_fine (1)",
                             "one_chute_is_not_fully_inflated_two_good_mains_landing_should_be_fine (2)"],
        "\"One chute is not fully inflated. Two good mains. Landing should be fine.\" The chutes' glitch on "
        "the descent (descent-chutes), placed to hear how it feels: the picture shows one main chute, the "
        "line speaks of three; Mercury's and Vostok's descents have no call."),
    "radio-hard-landing": (["recovery_reports_a_hard_landing_vehicle_is_ok (1)",
                            "recovery_reports_a_hard_landing_vehicle_is_ok (2)"]
                           + takes("recovery_reports_a_hard_landing_crew_is_okay", 3),
        "\"Recovery reports a hard landing. Vehicle is OK.\" / \"… Crew is okay.\" A landing on the ground that tips the capsule "
        "over: the glitches of the descent and landing and of the landing and recovery."),
    "radio-program-alarm": (takes("we_have_a_program_alarm_we_are_go_on_that_alarm", 3),
        "\"We have a program alarm. … We're go on that alarm.\" Apollo 11's 1202 alarm: the powered descent's "
        "glitch."),
    "radio-cutoff": (takes("hold_hold_hold_we_have_a_cutoff", 2),
        "\"Hold, hold, hold. We have a cutoff.\" The countdown's failure (the engine lights early), at the reveal."),
    "radio-launch-failure": (takes("abort_abort_abort", 2) + takes("we_have_lost_thrust_range_safety_is_standing_by", 2),
        "\"Abort. Abort. Abort.\" / \"We've lost thrust. Range safety is standing by.\" The launch's failure (the "
        "engine quits off the pad), at its reveal."),
    "radio-breakup": (takes("we_have_a_major_malfunction", 4) + takes("range_safety_has_initiated_destruct", 2),
        "\"We have a major malfunction.\" / \"Range safety has initiated destruct.\" The ascent's failure (the "
        "rocket breaks up at staging), at its reveal."),
    "radio-data-lost": (takes("flight_we_have_lost_all_data", 2) + takes("we_have_no_downlink", 3)
                        + takes("no_signal_we_have_lost_the_spacecraft", 4)
                        + takes("loss_of_signal_still_no_signal_no_acquisition", 6),
        "\"Flight, we've lost all data.\" / \"We have no downlink.\" / \"No signal. We've lost the spacecraft.\" / "
        "\"Loss of signal. … Still no signal. … No acquisition.\" A craft in orbit falling silent: the failures "
        "of the Earth orbit, the experiments, the biometric data and the carrier entry."),
    "radio-pressure": (takes("pressure_is_dropping_fast", 4),
        "\"Pressure is dropping fast.\" The long flights' failures (the crew comes home early)."),
    "radio-problem": (takes("flight_we_have_had_a_problem_here", 2),
        "\"Flight, we've had a problem here.\" (Apollo 13, reworded.) The coasts to and from the Moon, their "
        "glitches."),
    "radio-off-trajectory": (takes("flight_the_vehicle_is_off_trajectory", 2),
        "\"Flight, the vehicle is off trajectory.\" A correction that misfires: the failures of the coasts, the "
        "cruises between planets, the lunar approach and the lunar impact."),
    "radio-collision": (takes("flight_we_have_a_collision", 4),
        "\"Flight, we have a collision.\" The deployment's failure (the stage runs into the payload)."),
    "radio-closing-fast": (takes("closing_rate_is_high_recommend_you_brake_rates_look_good_now", 3),
        "\"Closing rate is high. Recommend you brake. … Rates look good now.\" The target approach's glitch (the "
        "braking puff comes late)."),
    "radio-latches": (takes("capture_latches_did_not_engage_back_off_and_try_again_hard_dock", 2),
        "\"Capture latches did not engage. Back off and try again. … Hard dock.\" The docking's glitch (a bounce, "
        "then a second try)."),
    "radio-abort-docking": (takes("abort_the_docking_back_away", 2),
        "\"Abort the docking. Back away.\" The docking's failure."),
    "radio-separation-failed": (takes("separation_has_failed_the_payload_is_still_attached", 4),
        "\"Separation has failed. The payload is still attached.\" Something that won't let go: the failures of "
        "the retropack's jettison, the modules' separation, the probe's release and the lander's extraction."),
    "radio-burn-failed": (takes("flight_the_burn_did_not_take_place", 3),
        "\"Flight, the burn did not take place.\" A burn that dies: the failures of the retrofire and of the "
        "burns into and out of lunar orbit; the Mars lander's descent's (its engines never light)."),
    "radio-tumbling": (takes("flight_we_are_tumbling_we_cannot_stop_it", 2),
        "\"Flight, we're tumbling. We cannot stop it.\" (Gemini 8.) A jet stuck open: the failures of the "
        "suborbital flight, the reentry preparations and the undocking."),
    "radio-blackout": (takes("we_are_in_blackout_expect_acquisition_in_about_two_minutes", 3),
        "\"We're in blackout. … Expect acquisition in about two minutes.\" The reentry's opening, in every "
        "variant (it tells nothing)."),
    "radio-no-acquisition": (takes("we_are_past_the_expected_acquisition_time_still_no_signal", 3),
        "\"We're past the expected acquisition time. Still no signal.\" The reentry's failure."),
    "radio-main-chute-failed": (["main_chute_did_not_deploy (1)", "main_chute_did_not_deploy (2)"],
        "\"Main chute did not deploy.\" The chutes' failures: the descents under chutes, to the ground or the "
        "sea."),
    "radio-recovery-no-contact": (takes("recovery_has_no_contact", 2),
        "\"Recovery has no contact.\" The touchdowns' failures (the main tears loose), on the ground or at sea."),
    "radio-capsule-sinking": (takes("flight_the_capsule_is_sinking", 2),
        "\"Flight, the capsule is sinking.\" (Liberty Bell 7.) The recovery's failure."),
    "radio-crew-back-in": (takes("he_is_having_trouble_getting_back_in_hatch_is_closed_he_is_in", 1),
        "\"He's having trouble getting back in. … Hatch is closed. He's in.\" A walk outside called off: the "
        "failures of the spacewalk, the moonwalk and the crew transfer."),
    "radio-abort-landing": (takes("abort_the_landing_abort", 3),
        "\"Abort the landing. Abort.\" The powered descent's failure (the crew aborts to orbit)."),
    "radio-contact-light": (takes("sixty_seconds_thirty_seconds_contact_light", 3),
        "\"Sixty seconds. … Thirty seconds. … Contact light.\" (Apollo 11's fuel calls.) The lunar landing's "
        "glitch (a hover low on fuel)."),
    "radio-lander-lost": (takes("we_have_lost_contact_with_the_lander", 4),
        "\"We've lost contact with the lander.\" The landings' failures, on the Moon and the planets; the "
        "landers' entries' at Venus and Mars, and Venera's descent's (its chute torn away)."),
    "radio-probe-silent": (takes("the_probe_has_stopped_transmitting", 2),
        "\"The probe has stopped transmitting.\" The failure of the work on the surface (Mars 3)."),
    "radio-drogue-late": (takes("drogue_is_late_drogue_deployed_main_chute_is_out", 3),
        "\"Drogue is late. … Drogue deployed. Main chute is out.\" Not placed yet: no clip shows a late drogue."),
    "radio-pad-shutdown": (takes("engine_shutdown_vehicle_is_safe_on_the_pad", 3),
        "\"Engine shutdown. Vehicle is safe on the pad.\" Not placed yet: no clip shuts down safely on the pad."),
    "radio-escape-fired": (takes("the_launch_escape_system_has_fired_crew_capsule_is_clear", 3),
        "\"The launch escape system has fired. Crew capsule is clear.\" Not placed yet: the clips don't know "
        "whether a crew flies."),
}

COMMON = (" Radio: a flight controller's call on the loop, VibeVoice in ComfyUI (the user's workflow), "
          "brownian noise under it, the Quindar tones at both ends (assets-raw/audio/chatter/). Streamed; it never overlaps another.")


def main():
    catalog = json.load(open(CATALOG, encoding="utf-8"))
    catalog["sounds"] = [s for s in catalog["sounds"] if not s["id"].startswith("radio-")]
    for old in os.listdir(DST):  # every take is copied afresh, so a renamed sound leaves nothing behind
        if old.startswith("radio-") and old.endswith(".mp3"):
            os.remove(os.path.join(DST, old))
    for sid, (sources, said) in SOUNDS.items():
        files = []
        for i, src in enumerate(sources, 1):
            name = f"{sid}-{i}.mp3" if len(sources) > 1 else f"{sid}.mp3"
            shutil.copyfile(os.path.join(SRC, src + ".mp3"), os.path.join(DST, name))
            files.append("audio/replay/" + name)
        catalog["sounds"].append({"id": sid, "kind": "RADIO", "volume": 0.7, "files": files, "credit": True,
                                  "description": said + COMMON})
    with open(CATALOG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        f.write("\n")
    used = {s for srcs, _ in SOUNDS.values() for s in srcs}
    left = [f for f in os.listdir(SRC) if f.endswith(".mp3") and f[:-4] not in used]
    print("unused:", left)


if __name__ == "__main__":
    main()
