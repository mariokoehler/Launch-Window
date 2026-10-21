"""Publishes the radio call-outs: python assets-raw/audio/chatter/make_radio_sounds.py

Copies the takes in this folder (made with VibeVoice in ComfyUI, the user's workflow; the user's own, or
Claude's from 2026-10-15, screened with Whisper and picked by the user's ear; each file's name is what's
said, "(n)" a variation of one line) into assets/audio/replay/ as radio-<id>[-n].mp3, one sound per
meaning, and writes their entries into assets/content/replay-sounds.json (any radio-* entries there are
replaced). Takes of one line ("(n)") get a "lines" list, so the replay never says the same words twice in a
row. Where they play is in assets-raw/replay/cues.json. A new take: add its file name to its sound
below, rerun, and list it in assets/CREDITS.md's radio bullet.
"""
import json
import os
import re
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
    "radio-guidance-internal": (takes("guidance_is_internal", 3),
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
        "landers' descents at Venus and Mars, their successes; the Mars landing's (the dish sending home)."),
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
        "\"The probe has stopped transmitting.\" The failure of the work on the surface (Mars 3), the probe's "
        "and Viking's lander's at Mars."),
    "radio-drogue-late": (takes("drogue_is_late_drogue_deployed_main_chute_is_out", 3),
        "\"Drogue is late. … Drogue deployed. Main chute is out.\" Not placed yet: no clip shows a late drogue."),
    "radio-pad-shutdown": (takes("engine_shutdown_vehicle_is_safe_on_the_pad", 3),
        "\"Engine shutdown. Vehicle is safe on the pad.\" Not placed yet: no clip shuts down safely on the pad."),
    "radio-escape-fired": (takes("the_launch_escape_system_has_fired_crew_capsule_is_clear", 3),
        "\"The launch escape system has fired. Crew capsule is clear.\" Not placed yet: the clips don't know "
        "whether a crew flies."),
}


# Round 2 (2026-10-21, round2.json): Claude's takes, picked by the user's ear. New sounds, and more lines for
# sounds above (their takes added, their descriptions extended).
ROUND_2 = {
    "radio-pad-glitch": (["we_have_a_leak_at_the_lower_arm_valve_is_closed_fueling_has_resumed (1)", "we_have_a_leak_at_the_lower_arm_valve_is_closed_fueling_has_resumed (2)", "propellant_leak_at_the_tower_the_leak_is_isolated_were_back_on_the_timeline (1)", "propellant_leak_at_the_tower_the_leak_is_isolated_were_back_on_the_timeline (2)", "propellant_leak_at_the_tower_the_leak_is_isolated_were_back_on_the_timeline (3)", "stop_the_fueling_okay_the_leak_is_fixed_resume_fueling (1)", "stop_the_fueling_okay_the_leak_is_fixed_resume_fueling (2)"],
        "\"We have a leak at the lower arm. Valve is closed. Fueling has resumed.\" / \"Propellant leak at the tower. The leak is isolated. We're back on the timeline.\" / \"Stop the fueling. Okay, the leak is fixed. Resume fueling.\" The glitches of launch-preparations, at the reveal."),
    "radio-pad-fire": (["we_have_a_fire_at_the_base_of_the_vehicle (1)", "we_have_a_fire_at_the_base_of_the_vehicle (2)", "flight_pad_fire_on_the_pad_safe_the_vehicle (1)", "flight_pad_fire_on_the_pad_safe_the_vehicle (2)", "flight_pad_fire_on_the_pad_safe_the_vehicle (3)", "fire_at_the_lower_arm_were_scrubbing (1)", "fire_at_the_lower_arm_were_scrubbing (2)", "fire_at_the_lower_arm_were_scrubbing (3)", "fire_at_the_lower_arm_were_scrubbing (4)", "fire_at_the_lower_arm_were_scrubbing (5)"],
        "\"We have a fire at the base of the vehicle.\" / \"Flight, Pad. Fire on the pad. Safe the vehicle.\" / \"Fire at the lower arm. We're scrubbing.\" The failures of launch-preparations, at the reveal."),
    "radio-burn-good": (["that_was_a_good_burn_engine_cutoff_on_time (1)", "that_was_a_good_burn_engine_cutoff_on_time (2)", "that_was_a_good_burn_engine_cutoff_on_time (3)", "burn_complete_residuals_are_small (1)", "burn_complete_residuals_are_small (2)", "shutdown_velocity_is_right_on_the_money (1)", "shutdown_velocity_is_right_on_the_money (2)", "shutdown_velocity_is_right_on_the_money (3)"],
        "\"That was a good burn. Engine cutoff on time.\" / \"Burn complete. Residuals are small.\" / \"Shutdown. Velocity is right on the money.\" The successes of high-orbit-injection, mars-orbit-injection, rendezvous-target-approach, tpi, at the reveal."),
    "radio-tli-good": (["the_injection_burn_is_complete_on_the_way_to_the_moon (1)", "the_injection_burn_is_complete_on_the_way_to_the_moon (2)", "the_injection_burn_is_complete_on_the_way_to_the_moon (3)", "the_injection_burn_is_complete_on_the_way_to_the_moon (4)", "the_injection_burn_is_complete_on_the_way_to_the_moon (5)", "good_burn_the_trajectory_is_right_on (1)", "good_burn_the_trajectory_is_right_on (2)", "good_burn_the_trajectory_is_right_on (3)", "good_burn_the_trajectory_is_right_on (4)", "engine_cutoff_we_have_escape_velocity (1)", "engine_cutoff_we_have_escape_velocity (2)", "engine_cutoff_we_have_escape_velocity (3)"],
        "\"The injection burn is complete. On the way to the Moon.\" / \"Good burn. The trajectory is right on.\" / \"Engine cutoff. We have escape velocity.\" The successes of tpi, at the reveal."),
    "radio-loi-good": (["acquisition_of_signal_we_have_a_good_orbit_around_the_moon (1)", "acquisition_of_signal_we_have_a_good_orbit_around_the_moon (2)", "acquisition_of_signal_we_have_a_good_orbit_around_the_moon (3)", "acquisition_of_signal_we_have_a_good_orbit_around_the_moon (4)", "signal_is_back_from_behind_the_moon_the_burn_was_good (1)", "signal_is_back_from_behind_the_moon_the_burn_was_good (2)", "signal_is_back_from_behind_the_moon_the_burn_was_good (3)", "the_insertion_burn_is_complete_good_orbit (1)", "the_insertion_burn_is_complete_good_orbit (2)"],
        "\"Acquisition of signal. We have a good orbit around the Moon.\" / \"Signal is back from behind the Moon. The burn was good.\" / \"The insertion burn is complete. Good orbit.\" The successes of loi, at the reveal."),
    "radio-tei-good": (["acquisition_of_signal_good_burn_its_on_the_way_home (1)", "acquisition_of_signal_good_burn_its_on_the_way_home (2)", "acquisition_of_signal_good_burn_its_on_the_way_home (3)", "acquisition_of_signal_good_burn_its_on_the_way_home (4)", "good_burn_for_home_its_on_its_way_back (1)", "good_burn_for_home_its_on_its_way_back (2)"],
        "\"Acquisition of signal. Good burn. It's on the way home.\" / \"Good burn for home. It's on its way back.\" The successes of tei, at the reveal."),
    "radio-retro-good": (["thats_retrofire_good_burn (1)", "thats_retrofire_good_burn (2)", "retros_fired_on_time_its_coming_down (1)", "retros_fired_on_time_its_coming_down (2)", "the_retro_burn_is_complete_trajectory_looks_good (1)", "the_retro_burn_is_complete_trajectory_looks_good (2)"],
        "\"That's retrofire. Good burn.\" / \"Retros fired on time. It's coming down.\" / \"The retro burn is complete. Trajectory looks good.\" The successes of deorbit-burn, deorbit-burn-om, retro-shutdown, retrofire, retrofire-om, at the reveal."),
    "radio-retro-glitch": (["one_retro_is_out_its_yawing_the_retro_is_back_burn_is_good (1)", "one_retro_is_out_its_yawing_the_retro_is_back_burn_is_good (2)", "one_retro_is_out_its_yawing_the_retro_is_back_burn_is_good (3)", "one_motor_is_out_the_other_two_are_holding_it_all_three_firing_again (1)", "one_motor_is_out_the_other_two_are_holding_it_all_three_firing_again (2)"],
        "\"One retro is out. It's yawing. The retro is back. Burn is good.\" / \"One motor is out. The other two are holding it. All three firing again.\" The glitches of deorbit-burn, deorbit-burn-om, retrofire, retrofire-om, at the reveal."),
    "radio-retro-shutdown-late": (["engine_is_still_burning_shutdown_its_stable_now (1)", "engine_is_still_burning_shutdown_its_stable_now (2)", "engine_is_still_burning_shutdown_its_stable_now (3)", "the_valve_is_sticking_engine_is_off_attitude_is_holding (1)", "the_valve_is_sticking_engine_is_off_attitude_is_holding (2)", "the_valve_is_sticking_engine_is_off_attitude_is_holding (3)"],
        "\"Engine is still burning. Shutdown. It's stable now.\" / \"The valve is sticking. Engine is off. Attitude is holding.\" The glitches of retro-shutdown, at the reveal."),
    "radio-retro-shutdown-failed": (["the_engine_wont_shut_down_its_pitching_away (1)", "the_engine_wont_shut_down_its_pitching_away (2)", "the_engine_wont_shut_down_its_pitching_away (3)", "no_shutdown_we_cant_hold_attitude (1)", "no_shutdown_we_cant_hold_attitude (2)", "no_shutdown_we_cant_hold_attitude (3)"],
        "\"The engine won't shut down. It's pitching away.\" / \"No shutdown. We can't hold attitude.\" The failures of retro-shutdown, at the reveal."),
    "radio-engine-test-good": (["engine_is_lit_and_stable_good_test (1)", "engine_is_lit_and_stable_good_test (2)", "good_firing_shutdown_on_time (1)", "good_firing_shutdown_on_time (2)", "throttle_response_is_good_the_engine_checks_out (1)", "throttle_response_is_good_the_engine_checks_out (2)"],
        "\"Engine is lit and stable. Good test.\" / \"Good firing. Shutdown on time.\" / \"Throttle response is good. The engine checks out.\" The successes of lander-engine-test, at the reveal."),
    "radio-engine-rough": (["engine_is_rough_we_have_a_master_alarm_now_its_smooth_burning_steady (1)", "engine_is_rough_we_have_a_master_alarm_now_its_smooth_burning_steady (2)", "engine_is_rough_we_have_a_master_alarm_now_its_smooth_burning_steady (3)", "chamber_pressure_is_fluctuating_its_settled_down (1)", "chamber_pressure_is_fluctuating_its_settled_down (2)", "chamber_pressure_is_fluctuating_its_settled_down (3)", "chamber_pressure_is_fluctuating_its_settled_down (4)", "chamber_pressure_is_fluctuating_its_settled_down (5)"],
        "\"Engine is rough. We have a master alarm. Now it's smooth. Burning steady.\" / \"Chamber pressure is fluctuating. It's settled down.\" The glitches of lander-engine-test, lander-staging, launch, rocket-climb, at the reveal."),
    "radio-engine-test-failed": (["engine_is_out_the_test_is_over (1)", "engine_is_out_the_test_is_over (2)", "weve_lost_the_engine_it_wont_restart (1)", "weve_lost_the_engine_it_wont_restart (2)", "weve_lost_the_engine_it_wont_restart (3)"],
        "\"Engine is out. The test is over.\" / \"We've lost the engine. It won't restart.\" The failures of lander-engine-test, at the reveal."),
    "radio-staging-good": (["we_have_staging_the_ascent_engine_is_good (1)", "we_have_staging_the_ascent_engine_is_good (2)", "we_have_staging_the_ascent_engine_is_good (3)", "fire_in_the_hole_clean_staging (1)", "fire_in_the_hole_clean_staging (2)"],
        "\"We have staging. The ascent engine is good.\" / \"Fire in the hole. Clean staging.\" The successes of lander-staging, at the reveal."),
    "radio-coast-good": (["midcourse_correction_is_complete_right_on_track (1)", "midcourse_correction_is_complete_right_on_track (2)", "midcourse_correction_is_complete_right_on_track (3)", "tracking_shows_a_good_trajectory (1)", "tracking_shows_a_good_trajectory (2)", "all_systems_are_nominal_a_quiet_day (1)", "all_systems_are_nominal_a_quiet_day (2)", "all_systems_are_nominal_a_quiet_day (3)"],
        "\"Midcourse correction is complete. Right on track.\" / \"Tracking shows a good trajectory.\" / \"All systems are nominal. A quiet day.\" The successes of earth-jupiter-transit, earth-mars-transit, earth-venus-transit, jupiter-saturn-transit, trans-earth-coast, trans-lunar-coast, venus-mercury-transit, at the reveal."),
    "radio-course-corrected": (["its_drifting_off_course_correction_burn_complete_back_on_track (1)", "its_drifting_off_course_correction_burn_complete_back_on_track (2)", "our_tracking_shows_an_error_in_the_trajectory_mid_course_correction_is_good (1)", "our_tracking_shows_an_error_in_the_trajectory_mid_course_correction_is_good (2)", "our_tracking_shows_an_error_in_the_trajectory_mid_course_correction_is_good (3)", "were_off_the_line_correction_is_in_right_on_the_target_again"],
        "\"It's drifting off course. Correction burn complete. Back on track.\" / \"Our tracking shows an error in the trajectory. Mid-course correction is good.\" / \"We're off the line. Correction is in. Right on the target again.\" The glitches of earth-jupiter-transit, earth-mars-transit, earth-venus-transit, jupiter-approach, jupiter-flyby, jupiter-saturn-transit, lunar-approach, lunar-flyby, lunar-impact, mars-approach, mars-flyby, mercury-flyby, saturn-approach, saturn-flyby, venus-approach, venus-flyby, venus-mercury-transit, at the reveal."),
    "radio-approach-good": (["final_correction_is_done_right_on_target (1)", "final_correction_is_done_right_on_target (2)", "navigation_looks_good_were_in_the_corridor (1)", "navigation_looks_good_were_in_the_corridor (2)", "approach_is_on_target_encounter_in_a_few_hours (1)", "approach_is_on_target_encounter_in_a_few_hours (2)"],
        "\"Final correction is done. Right on target.\" / \"Navigation looks good. We're in the corridor.\" / \"Approach is on target. Encounter in a few hours.\" The successes of jupiter-approach, lunar-approach, mars-approach, saturn-approach, venus-approach, at the reveal."),
    "radio-flyby-good": (["closest_approach_the_instruments_are_on (1)", "closest_approach_the_instruments_are_on (2)", "encounter_sequence_is_running_data_is_coming_in (1)", "encounter_sequence_is_running_data_is_coming_in (2)", "encounter_sequence_is_running_data_is_coming_in (3)", "encounter_sequence_is_running_data_is_coming_in (4)", "acquisition_of_signal_it_made_the_pass_heading_out"],
        "\"Closest approach. The instruments are on.\" / \"Encounter sequence is running. Data is coming in.\" / \"Acquisition of signal. It made the pass. Heading out.\" The successes of jupiter-flyby, lunar-flyby, mars-flyby, mercury-flyby, saturn-flyby, venus-flyby, at the reveal."),
    "radio-orbit-science": (["were_getting_good_data (1)", "were_getting_good_data (2)", "instruments_are_on_and_working (1)", "instruments_are_on_and_working (2)", "the_mapping_sequence_is_running_pictures_look_good (1)", "the_mapping_sequence_is_running_pictures_look_good (2)"],
        "\"We're getting good data.\" / \"Instruments are on and working.\" / \"The mapping sequence is running. Pictures look good.\" The successes of advanced-experiments-earth-orbit, biometric-data-transmission, lunar-orbit, mars-orbit, sun-measurements, venus-orbit, at the reveal."),
    "radio-safe-mode": (["the_spacecraft_is_in_safe_mode_we_have_it_back_resuming_the_sequence (1)", "the_spacecraft_is_in_safe_mode_we_have_it_back_resuming_the_sequence (2)", "the_spacecraft_is_in_safe_mode_we_have_it_back_resuming_the_sequence (3)", "weve_lost_attitude_lock_lock_is_back_data_is_flowing (1)", "weve_lost_attitude_lock_lock_is_back_data_is_flowing (2)"],
        "\"The spacecraft is in safe mode. We have it back. Resuming the sequence.\" / \"We've lost attitude lock. Lock is back. Data is flowing.\" The glitches of lunar-orbit, mars-orbit, sun-measurements, venus-orbit, at the reveal."),
    "radio-entry-corridor-good": (["entry_corridor_looks_good (1)", "entry_corridor_looks_good (2)", "right_in_the_corridor_entry_in_twenty_minutes (1)", "right_in_the_corridor_entry_in_twenty_minutes (2)"],
        "\"Entry corridor looks good.\" / \"Right in the corridor. Entry in twenty minutes.\" The successes of earth-approach, high-orbit-return, high-orbit-trim, at the reveal."),
    "radio-corridor-glitch": (["entry_angle_is_too_shallow_correction_is_in_back_in_the_corridor (1)", "entry_angle_is_too_shallow_correction_is_in_back_in_the_corridor (2)", "entry_angle_is_too_shallow_correction_is_in_back_in_the_corridor (3)", "its_drifting_high_trim_burn_is_good_were_in_the_corridor (1)", "its_drifting_high_trim_burn_is_good_were_in_the_corridor (2)", "its_drifting_high_trim_burn_is_good_were_in_the_corridor (3)"],
        "\"Entry angle is too shallow. Correction is in. Back in the corridor.\" / \"It's drifting high. Trim burn is good. We're in the corridor.\" The glitches of earth-approach, high-orbit-return, high-orbit-trim, at the reveal."),
    "radio-skip-out": (["its_too_shallow_it_will_skip_off_the_atmosphere (1)", "its_too_shallow_it_will_skip_off_the_atmosphere (2)", "the_correction_failed_its_above_the_corridor (1)", "the_correction_failed_its_above_the_corridor (2)"],
        "\"It's too shallow. It will skip off the atmosphere.\" / \"The correction failed. It's above the corridor.\" The failures of earth-approach, high-orbit-return, high-orbit-trim, at the reveal."),
    "radio-crew-well": (["crew_reports_all_is_well (1)", "crew_reports_all_is_well (2)", "crew_reports_all_is_well (3)", "another_good_day_up_there_consumables_are_on_the_plan (1)", "another_good_day_up_there_consumables_are_on_the_plan (2)", "another_good_day_up_there_consumables_are_on_the_plan (3)", "the_crew_is_in_good_shape_theyre_go_for_another_day", "everything_is_quiet_theyre_right_on_the_flight_plan (1)", "everything_is_quiet_theyre_right_on_the_flight_plan (2)", "everything_is_quiet_theyre_right_on_the_flight_plan (3)"],
        "\"Crew reports all is well.\" / \"Another good day up there. Consumables are on the plan.\" / \"The crew is in good shape. They're go for another day.\" / \"Everything is quiet. They're right on the flight plan.\" The successes of duration-level-i, duration-level-ii, lunar-orbit, at the reveal."),
    "radio-rendezvous-good": (["radar_lock_on_the_target_range_is_closing (1)", "radar_lock_on_the_target_range_is_closing (2)", "target_in_sight_closing_rate_is_good (1)", "target_in_sight_closing_rate_is_good (2)", "target_in_sight_closing_rate_is_good (3)", "station_keeping_rendezvous_is_complete (1)", "station_keeping_rendezvous_is_complete (2)", "station_keeping_rendezvous_is_complete (3)"],
        "\"Radar lock on the target. Range is closing.\" / \"Target in sight. Closing rate is good.\" / \"Station-keeping. Rendezvous is complete.\" The successes of rendezvous, target-approach, at the reveal."),
    "radio-station-keeping": (["its_drifting_back_correcting_holding_station_again (1)", "its_drifting_back_correcting_holding_station_again (2)", "its_drifting_back_correcting_holding_station_again (3)", "were_losing_the_target_okay_back_in_position (1)", "were_losing_the_target_okay_back_in_position (2)"],
        "\"It's drifting back. Correcting. Holding station again.\" / \"We're losing the target. Okay, back in position.\" The glitches of rendezvous, at the reveal."),
    "radio-rendezvous-lost": (["were_out_of_propellant_we_cant_hold_station (1)", "were_out_of_propellant_we_cant_hold_station (2)", "were_out_of_propellant_we_cant_hold_station (3)", "the_target_is_out_of_reach_rendezvous_is_lost (1)", "the_target_is_out_of_reach_rendezvous_is_lost (2)", "the_target_is_out_of_reach_rendezvous_is_lost (3)"],
        "\"We're out of propellant. We can't hold station.\" / \"The target is out of reach. Rendezvous is lost.\" The failures of rendezvous, rendezvous-target-approach, at the reveal."),
    "radio-cant-brake": (["the_brakes_arent_answering_abort_the_approach (1)", "the_brakes_arent_answering_abort_the_approach (2)", "the_brakes_arent_answering_abort_the_approach (3)", "we_have_no_braking_its_passing_the_target (1)", "we_have_no_braking_its_passing_the_target (2)"],
        "\"The brakes aren't answering. Abort the approach.\" / \"We have no braking. It's passing the target.\" The failures of target-approach, at the reveal."),
    "radio-docked": (["contact_capture_we_have_a_hard_dock (1)", "contact_capture_we_have_a_hard_dock (2)", "docking_is_complete_all_latches_are_engaged (1)", "docking_is_complete_all_latches_are_engaged (2)", "docking_is_complete_all_latches_are_engaged (3)", "docking_is_complete_all_latches_are_engaged (4)", "were_docked (1)", "were_docked (2)", "were_docked (3)", "were_docked (4)"],
        "\"Contact. Capture. We have a hard dock.\" / \"Docking is complete. All latches are engaged.\" / \"We're docked.\" The successes of docking, transposition, at the reveal."),
    "radio-undocked": (["undocking_is_complete_backing_away (1)", "undocking_is_complete_backing_away (2)", "undocking_is_complete_backing_away (3)", "clean_separation_from_the_target (1)", "clean_separation_from_the_target (2)", "latches_released_were_free (1)", "latches_released_were_free (2)"],
        "\"Undocking is complete. Backing away.\" / \"Clean separation from the target.\" / \"Latches released. We're free.\" The successes of extraction, undocking, at the reveal."),
    "radio-undock-glitch": (["the_latches_didnt_release_sending_it_again_were_free (1)", "the_latches_didnt_release_sending_it_again_were_free (2)", "no_separation_on_the_first_try_there_it_goes (1)", "no_separation_on_the_first_try_there_it_goes (2)", "no_separation_on_the_first_try_there_it_goes (3)"],
        "\"The latches didn't release. Sending it again. We're free.\" / \"No separation on the first try. There it goes.\" The glitches of extraction, undocking, at the reveal."),
    "radio-ingress-good": (["hes_at_the_other_hatch_going_in_now (1)", "hes_at_the_other_hatch_going_in_now (2)", "hes_back_in_hatch_is_closed (1)", "hes_back_in_hatch_is_closed (2)", "hes_back_in_hatch_is_closed (3)", "hatch_is_closed_and_locked_repressurizing (1)", "hatch_is_closed_and_locked_repressurizing (2)", "hatch_is_closed_and_locked_repressurizing (3)"],
        "\"He's at the other hatch. Going in now.\" / \"He's back in. Hatch is closed.\" / \"Hatch is closed and locked. Repressurizing.\" The successes of crew-transfer, eva-in, transfer-ingress, at the reveal."),
    "radio-egress-good": (["hatch_is_open_hes_outside (1)", "hatch_is_open_hes_outside (2)", "hatch_is_open_hes_outside (3)", "hatch_is_open_hes_outside (4)", "hatch_is_open_hes_outside (5)", "hes_out_on_the_tether_everything_looks_good (1)", "hes_out_on_the_tether_everything_looks_good (2)", "hes_out_on_the_tether_everything_looks_good (3)"],
        "\"Hatch is open. He's outside.\" / \"He's out on the tether. Everything looks good.\" The successes of eva-out, eva-out-standing, eva-standup, at the reveal."),
    "radio-eva-good": (["suit_pressure_is_holding_hes_on_the_timeline (1)", "suit_pressure_is_holding_hes_on_the_timeline (2)", "suit_pressure_is_holding_hes_on_the_timeline (3)", "suit_pressure_is_holding_hes_on_the_timeline (4)", "that_task_is_done_right_on_the_plan (1)", "that_task_is_done_right_on_the_plan (2)", "hes_moving_well_no_problems (1)", "hes_moving_well_no_problems (2)", "hes_moving_well_no_problems (3)", "thats_done_hes_ready_for_the_next_task (1)", "thats_done_hes_ready_for_the_next_task (2)", "thats_done_hes_ready_for_the_next_task (3)"],
        "\"Suit pressure is holding. He's on the timeline.\" / \"That task is done. Right on the plan.\" / \"He's moving well. No problems.\" / \"That's done. He's ready for the next task.\" The successes of eva, eva-experiments, eva-film, eva-pair, eva-photo, eva-rail, eva-setup, eva-teardown, at the reveal."),
    "radio-eva-tumble": (["hes_tumbling_on_the_tether_hes_got_it_under_control (1)", "hes_tumbling_on_the_tether_hes_got_it_under_control (2)", "he_lost_his_grip_okay_hes_back_on_the_rail (1)", "he_lost_his_grip_okay_hes_back_on_the_rail (2)", "hes_drifting_away_the_gun_stopped_him_hes_fine (1)", "hes_drifting_away_the_gun_stopped_him_hes_fine (2)"],
        "\"He's tumbling on the tether. He's got it under control.\" / \"He lost his grip. Okay, he's back on the rail.\" / \"He's drifting away. The gun stopped him. He's fine.\" The glitches of crew-transfer, eva, eva-out, eva-out-standing, eva-pair, eva-rail, at the reveal."),
    "radio-eva-trouble": (["its_stuck_a_good_push_its_free (1)", "its_stuck_a_good_push_its_free (2)", "his_suit_is_stiff_hes_working_hard_he_made_it (1)", "his_suit_is_stiff_hes_working_hard_he_made_it (2)"],
        "\"It's stuck. A good push. It's free.\" / \"His suit is stiff. He's working hard. He made it.\" The glitches of eva-experiments, eva-film, eva-in, eva-photo, eva-standup, eva-teardown, transfer-ingress, at the reveal."),
    "radio-eva-task-failed": (["weve_lost_that_task_leave_it_and_move_on (1)", "weve_lost_that_task_leave_it_and_move_on (2)", "its_not_going_to_work_scrub_the_task (1)", "its_not_going_to_work_scrub_the_task (2)", "its_not_going_to_work_scrub_the_task (3)"],
        "\"We've lost that task. Leave it and move on.\" / \"It's not going to work. Scrub the task.\" The failures of eva-experiments, eva-film, eva-photo, eva-standup, eva-teardown, transfer-ingress, at the reveal."),
    "radio-moonwalk-good": (["theyre_right_on_the_timeline (1)", "theyre_right_on_the_timeline (2)", "theyre_right_on_the_timeline (3)", "thats_a_good_sample_into_the_bag (1)", "thats_a_good_sample_into_the_bag (2)", "thats_a_good_sample_into_the_bag (3)", "the_flag_is_up (1)", "the_flag_is_up (2)", "the_flag_is_up (3)", "experiments_are_out_and_working (1)", "experiments_are_out_and_working (2)", "experiments_are_out_and_working (3)", "experiments_are_out_and_working (4)"],
        "\"They're right on the timeline.\" / \"That's a good sample. Into the bag.\" / \"The flag is up.\" / \"Experiments are out and working.\" The successes of flag, moonwalk, samples, at the reveal."),
    "radio-moonwalk-fall": (["hes_down_hes_up_again_no_harm_done (1)", "hes_down_hes_up_again_no_harm_done (2)", "the_ground_is_hard_here_second_try_its_in (1)", "the_ground_is_hard_here_second_try_its_in (2)", "the_ground_is_hard_here_second_try_its_in (3)", "he_dropped_a_sample_hes_picked_it_up_again"],
        "\"He's down. He's up again. No harm done.\" / \"The ground is hard here. Second try. It's in.\" / \"He dropped a sample. He's picked it up again.\" The glitches of flag, moonwalk, samples, at the reveal."),
    "radio-rover-good": (["the_rover_is_rolling_theyre_on_their_way (1)", "the_rover_is_rolling_theyre_on_their_way (2)", "the_rover_is_rolling_theyre_on_their_way (3)", "theyre_at_the_next_stop (1)", "theyre_at_the_next_stop (2)", "theyre_at_the_next_stop (3)", "theyre_at_the_next_stop (4)"],
        "\"The rover is rolling. They're on their way.\" / \"They're at the next stop.\" The successes of rover, at the reveal."),
    "radio-rover-glitch": (["they_lost_a_fender_its_taped_up_theyre_rolling_again (1)", "they_lost_a_fender_its_taped_up_theyre_rolling_again (2)", "they_lost_a_fender_its_taped_up_theyre_rolling_again (3)", "that_was_a_hard_bump_rover_checks_out_moving_on (1)", "that_was_a_hard_bump_rover_checks_out_moving_on (2)", "that_was_a_hard_bump_rover_checks_out_moving_on (3)"],
        "\"They lost a fender. It's taped up. They're rolling again.\" / \"That was a hard bump. Rover checks out. Moving on.\" The glitches of rover, at the reveal."),
    "radio-rover-dead": (["the_rover_has_quit_theyre_walking_back (1)", "the_rover_has_quit_theyre_walking_back (2)", "the_rover_has_quit_theyre_walking_back (3)", "the_rover_has_quit_theyre_walking_back (4)", "no_power_on_the_rover_theyre_leaving_it (1)", "no_power_on_the_rover_theyre_leaving_it (2)", "no_power_on_the_rover_theyre_leaving_it (3)"],
        "\"The rover has quit. They're walking back.\" / \"No power on the rover. They're leaving it.\" The failures of rover, at the reveal."),
    "radio-separation-good": (["separation_confirmed (1)", "separation_confirmed (2)", "clean_separation (1)", "clean_separation (2)", "clean_separation (3)", "clean_separation (4)", "pyros_fired_were_clear (1)", "pyros_fired_were_clear (2)", "pyros_fired_were_clear (3)"],
        "\"Separation confirmed.\" / \"Clean separation.\" / \"Pyros fired. We're clear.\" The successes of equipment-section-separation, modules-separation, orbital-module-separation, retrofire-jettison, retrofire-jettison-om, at the reveal."),
    "radio-lander-away": (["lander_separation_confirmed_its_on_its_own (1)", "lander_separation_confirmed_its_on_its_own (2)", "the_probe_is_away_good_separation (1)", "the_probe_is_away_good_separation (2)", "the_probe_is_away_good_separation (3)", "the_probe_is_away_good_separation (4)"],
        "\"Lander separation confirmed. It's on its own.\" / \"The probe is away. Good separation.\" The successes of multiprobe-large-release, multiprobe-small-release, probe-release, viking-separation, at the reveal."),
    "radio-over-the-top": (["its_over_the_top_coming_down_now", "over_the_top_nice_and_steady (1)", "over_the_top_nice_and_steady (2)", "over_the_top_nice_and_steady (3)"],
        "\"It's over the top. Coming down now.\" / \"Over the top. Nice and steady.\" The successes of suborbital-flight, suborbital-flight-om, x15-peak, at the reveal."),
    "radio-main-good": (["drogue_is_out_main_chute_is_good (1)", "drogue_is_out_main_chute_is_good (2)", "drogue_is_out_main_chute_is_good (3)", "drogue_is_out_main_chute_is_good (4)", "we_have_a_good_main_descent_rate_is_normal (1)", "we_have_a_good_main_descent_rate_is_normal (2)", "chutes_are_out_everything_looks_good (1)", "chutes_are_out_everything_looks_good (2)", "chutes_are_out_everything_looks_good (3)", "chutes_are_out_everything_looks_good (4)"],
        "\"Drogue is out. Main chute is good.\" / \"We have a good main. Descent rate is normal.\" / \"Chutes are out. Everything looks good.\" The successes of descent-and-landing, descent-and-splashdown, descent-chutes, at the reveal."),
    "radio-splashdown-good": (["splashdown_recovery_has_it_in_sight (1)", "splashdown_recovery_has_it_in_sight (2)", "we_have_splashdown_the_helicopter_is_on_the_way (1)", "we_have_splashdown_the_helicopter_is_on_the_way (2)"],
        "\"Splashdown. Recovery has it in sight.\" / \"We have splashdown. The helicopter is on the way.\" The successes of splashdown-and-recovery, at the reveal."),
    "radio-landed-good": (["touchdown_recovery_crews_are_on_the_way (1)", "touchdown_recovery_crews_are_on_the_way (2)", "touchdown_recovery_crews_are_on_the_way (3)", "its_down_the_beacon_is_on (1)", "its_down_the_beacon_is_on (2)"],
        "\"Touchdown. Recovery crews are on the way.\" / \"It's down. The beacon is on.\" The successes of landing-and-recovery, at the reveal."),
    "radio-recovery-good": (["recovery_is_complete_its_on_board (1)", "recovery_is_complete_its_on_board (2)", "the_flotation_collar_is_on_ready_for_the_lift (1)", "the_flotation_collar_is_on_ready_for_the_lift (2)"],
        "\"Recovery is complete. It's on board.\" / \"The flotation collar is on. Ready for the lift.\" The successes of recovery, at the reveal."),
    "radio-recovery-list": (["its_listing_badly_the_collar_is_on_its_stable (1)", "its_listing_badly_the_collar_is_on_its_stable (2)", "the_capsule_is_taking_on_water_swimmers_have_the_flotation_collar_on (1)", "the_capsule_is_taking_on_water_swimmers_have_the_flotation_collar_on (2)", "the_capsule_is_taking_on_water_swimmers_have_the_flotation_collar_on (3)"],
        "\"It's listing badly. The collar is on. It's stable.\" / \"The capsule is taking on water. Swimmers have the flotation collar on.\" The glitches of recovery, splashdown-and-recovery, at the reveal."),
    "radio-entry-good": (["entry_looks_good_the_signal_is_steady (1)", "entry_looks_good_the_signal_is_steady (2)", "entry_looks_good_the_signal_is_steady (3)", "entry_looks_good_the_signal_is_steady (4)", "peak_heating_is_past_its_holding_together (1)", "peak_heating_is_past_its_holding_together (2)"],
        "\"Entry looks good. The signal is steady.\" / \"Peak heating is past. It's holding together.\" The successes of mars-entry, venus-entry, x15-descent, at the reveal."),
    "radio-lander-down": (["we_have_signal_from_the_surface (1)", "we_have_signal_from_the_surface (2)", "we_have_signal_from_the_surface (3)", "the_lander_is_down_and_transmitting (1)", "the_lander_is_down_and_transmitting (2)", "the_lander_is_down_and_transmitting (3)", "the_lander_is_down_and_transmitting (4)", "touchdown_signal_from_the_surface (1)", "touchdown_signal_from_the_surface (2)", "touchdown_signal_from_the_surface (3)"],
        "\"We have signal from the surface.\" / \"The lander is down and transmitting.\" / \"Touchdown. Signal from the surface.\" The successes of probe-landing; the glitches of mars-landing, probe-landing, at the reveal."),
    "radio-descent-good": (["altitude_and_velocity_look_good (1)", "altitude_and_velocity_look_good (2)", "altitude_and_velocity_look_good (3)", "landing_radar_has_lock_go_for_landing (1)", "landing_radar_has_lock_go_for_landing (2)", "descent_engine_is_good_right_on_the_profile (1)", "descent_engine_is_good_right_on_the_profile (2)", "descent_engine_is_good_right_on_the_profile (3)"],
        "\"Altitude and velocity look good.\" / \"Landing radar has lock. Go for landing.\" / \"Descent engine is good. Right on the profile.\" The successes of powered-descent, at the reveal."),
    "radio-moon-touchdown": (["touchdown_engine_stop (1)", "touchdown_engine_stop (2)", "touchdown_engine_stop (3)", "contact_its_down_on_the_surface (1)", "contact_its_down_on_the_surface (2)", "contact_its_down_on_the_surface (3)", "contact_its_down_on_the_surface (4)", "contact_its_down_on_the_surface (5)", "we_have_touchdown_the_signal_is_strong (1)", "we_have_touchdown_the_signal_is_strong (2)", "we_have_touchdown_the_signal_is_strong (3)"],
        "\"Touchdown. Engine stop.\" / \"Contact. It's down on the surface.\" / \"We have touchdown. The signal is strong.\" The successes of lunar-landing, at the reveal."),
    "radio-impact-good": (["pictures_are_coming_in_right_up_to_impact (1)", "pictures_are_coming_in_right_up_to_impact (2)", "pictures_are_coming_in_right_up_to_impact (3)", "pictures_are_coming_in_right_up_to_impact (4)", "impact_good_pictures_to_the_end (1)", "impact_good_pictures_to_the_end (2)"],
        "\"Pictures are coming in, right up to impact.\" / \"Impact. Good pictures to the end.\" The successes of lunar-impact, at the reveal."),
    "radio-liftoff-good": (["we_have_liftoff_the_ascent_engine_is_good (1)", "we_have_liftoff_the_ascent_engine_is_good (2)", "its_climbing_well_right_on_the_trajectory", "its_off_the_surface_and_climbing (1)", "its_off_the_surface_and_climbing (2)"],
        "\"We have liftoff. The ascent engine is good.\" / \"It's climbing well. Right on the trajectory.\" / \"It's off the surface and climbing.\" The successes of lunar-ascent, lunar-liftoff, at the reveal."),
    "radio-liftoff-late": (["ascent_engine_is_slow_to_light_there_it_goes_liftoff (1)", "ascent_engine_is_slow_to_light_there_it_goes_liftoff (2)", "ascent_engine_is_slow_to_light_there_it_goes_liftoff (3)", "no_ignition_yet_ignition_its_climbing (1)", "no_ignition_yet_ignition_its_climbing (2)", "no_ignition_yet_ignition_its_climbing (3)"],
        "\"Ascent engine is slow to light. There it goes. Liftoff.\" / \"No ignition yet. Ignition. It's climbing.\" The glitches of luna-liftoff, lunar-liftoff, at the reveal."),
    "radio-liftoff-failed": (["the_ascent_engine_has_quit (1)", "the_ascent_engine_has_quit (2)", "weve_lost_the_ascent_stage (1)", "weve_lost_the_ascent_stage (2)", "weve_lost_the_ascent_stage (3)"],
        "\"The ascent engine has quit.\" / \"We've lost the ascent stage.\" The failures of lunar-ascent, lunar-liftoff, at the reveal."),
    "radio-surface-good": (["the_sampler_arm_is_working (1)", "the_sampler_arm_is_working (2)", "the_sampler_arm_is_working (3)", "we_have_pictures_from_the_surface_they_look_good (1)", "we_have_pictures_from_the_surface_they_look_good (2)", "we_have_pictures_from_the_surface_they_look_good (3)", "everything_on_the_lander_checks_out (1)", "everything_on_the_lander_checks_out (2)", "everything_on_the_lander_checks_out (3)", "everything_on_the_lander_checks_out (4)", "everything_on_the_lander_checks_out (5)"],
        "\"The sampler arm is working.\" / \"We have pictures from the surface. They look good.\" / \"Everything on the lander checks out.\" The successes of mars-surface, probe-surface, at the reveal."),
    "radio-plane-takeoff": (["the_carrier_is_airborne (1)", "the_carrier_is_airborne (2)", "wheels_up_climbing_to_drop_altitude (1)", "wheels_up_climbing_to_drop_altitude (2)", "wheels_up_climbing_to_drop_altitude (3)", "wheels_up_climbing_to_drop_altitude (4)", "wheels_up_climbing_to_drop_altitude (5)"],
        "\"The carrier is airborne.\" / \"Wheels up. Climbing to drop altitude.\" The successes of air-takeoff, at the reveal."),
    "radio-plane-takeoff-glitch": (["engine_three_is_smoking_its_airborne_a_little_late (1)", "engine_three_is_smoking_its_airborne_a_little_late (2)", "engine_three_is_smoking_its_airborne_a_little_late (3)", "slow_on_the_roll_okay_its_off_the_ground (1)", "slow_on_the_roll_okay_its_off_the_ground (2)", "slow_on_the_roll_okay_its_off_the_ground (3)"],
        "\"Engine three is smoking. It's airborne, a little late.\" / \"Slow on the roll. Okay, it's off the ground.\" The glitches of air-takeoff, at the reveal."),
    "radio-plane-takeoff-abort": (["abort_the_takeoff_theyre_braking_on_the_runway (1)", "abort_the_takeoff_theyre_braking_on_the_runway (2)", "takeoff_is_off_bring_it_to_a_stop (1)", "takeoff_is_off_bring_it_to_a_stop (2)"],
        "\"Abort the takeoff. They're braking on the runway.\" / \"Takeoff is off. Bring it to a stop.\" The failures of air-takeoff, at the reveal."),
    "radio-plane-cruise": (["ten_minutes_to_drop_everything_is_green (1)", "ten_minutes_to_drop_everything_is_green (2)", "ten_minutes_to_drop_everything_is_green (3)", "ten_minutes_to_drop_everything_is_green (4)", "approaching_the_drop_point_the_pilot_reports_ready (1)", "approaching_the_drop_point_the_pilot_reports_ready (2)", "approaching_the_drop_point_the_pilot_reports_ready (3)"],
        "\"Ten minutes to drop. Everything is green.\" / \"Approaching the drop point. The pilot reports ready.\" The successes of air-cruise, at the reveal."),
    "radio-plane-cruise-glitch": (["we_have_a_caution_light_its_cleared_continue (1)", "we_have_a_caution_light_its_cleared_continue (2)", "the_pilot_reports_a_warning_light_its_gone_out_were_go (1)", "the_pilot_reports_a_warning_light_its_gone_out_were_go (2)"],
        "\"We have a caution light. It's cleared. Continue.\" / \"The pilot reports a warning light. It's gone out. We're go.\" The glitches of air-cruise, at the reveal."),
    "radio-plane-cruise-abort": (["the_drop_is_off_theyre_turning_for_home (1)", "the_drop_is_off_theyre_turning_for_home (2)", "the_drop_is_off_theyre_turning_for_home (3)", "no_drop_today_the_light_is_still_on (1)", "no_drop_today_the_light_is_still_on (2)", "no_drop_today_the_light_is_still_on (3)"],
        "\"The drop is off. They're turning for home.\" / \"No drop today. The light is still on.\" The failures of air-cruise, at the reveal."),
    "radio-plane-drop": (["we_have_a_drop_good_light_on_the_engine (1)", "we_have_a_drop_good_light_on_the_engine (2)", "clean_drop_the_engine_is_burning (1)", "clean_drop_the_engine_is_burning (2)", "clean_drop_the_engine_is_burning (3)"],
        "\"We have a drop. Good light on the engine.\" / \"Clean drop. The engine is burning.\" The successes of air-drop, at the reveal."),
    "radio-plane-drop-glitch": (["no_light_relight_good_engine (1)", "no_light_relight_good_engine (2)", "the_engine_didnt_start_second_try_its_lit (1)", "the_engine_didnt_start_second_try_its_lit (2)", "the_engine_didnt_start_second_try_its_lit (3)", "the_engine_didnt_start_second_try_its_lit (4)"],
        "\"No light. Relight. Good engine.\" / \"The engine didn't start. Second try. It's lit.\" The glitches of air-drop, at the reveal."),
    "radio-plane-drop-failed": (["no_engine_hes_gliding_down_to_the_lake_bed (1)", "no_engine_hes_gliding_down_to_the_lake_bed (2)", "the_engine_wont_light_hes_heading_for_an_emergency_lake (1)", "the_engine_wont_light_hes_heading_for_an_emergency_lake (2)"],
        "\"No engine. He's gliding down to the lake bed.\" / \"The engine won't light. He's heading for an emergency lake.\" The failures of air-drop, at the reveal."),
    "radio-plane-climb": (["good_climb_right_on_the_profile (1)", "good_climb_right_on_the_profile (2)", "good_climb_right_on_the_profile (3)", "good_climb_right_on_the_profile (4)", "engine_burnout_hes_coasting_up"],
        "\"Good climb. Right on the profile.\" / \"Engine burnout. He's coasting up.\" The successes of rocket-climb, at the reveal."),
    "radio-plane-climb-failed": (["engine_shutdown_hes_heading_for_an_emergency_lake (1)", "engine_shutdown_hes_heading_for_an_emergency_lake (2)", "engine_shutdown_hes_heading_for_an_emergency_lake (3)", "hes_lost_the_engine_chase_is_with_him (1)", "hes_lost_the_engine_chase_is_with_him (2)"],
        "\"Engine shutdown. He's heading for an emergency lake.\" / \"He's lost the engine. Chase is with him.\" The failures of rocket-climb, x15-fast, at the reveal."),
    "radio-plane-fast-good": (["top_speed_temperatures_are_holding (1)", "top_speed_temperatures_are_holding (2)", "hes_at_full_speed_everything_looks_good (1)", "hes_at_full_speed_everything_looks_good (2)", "hes_at_full_speed_everything_looks_good (3)"],
        "\"Top speed. Temperatures are holding.\" / \"He's at full speed. Everything looks good.\" The successes of x15-fast, at the reveal."),
    "radio-plane-hot-spot": (["hot_spot_on_the_belly_hes_throttling_back_its_cooling_down (1)", "hot_spot_on_the_belly_hes_throttling_back_its_cooling_down (2)", "hot_spot_on_the_belly_hes_throttling_back_its_cooling_down (3)", "hot_spot_on_the_belly_hes_throttling_back_its_cooling_down (4)", "skin_temperature_is_high_hes_slowed_down_its_fine_now (1)", "skin_temperature_is_high_hes_slowed_down_its_fine_now (2)", "skin_temperature_is_high_hes_slowed_down_its_fine_now (3)"],
        "\"Hot spot on the belly. He's throttling back. It's cooling down.\" / \"Skin temperature is high. He's slowed down. It's fine now.\" The glitches of x15-fast, at the reveal."),
    "radio-runway-good": (["touchdown_on_the_lake_bed_nice_landing (1)", "touchdown_on_the_lake_bed_nice_landing (2)", "the_skids_are_down_hes_rolling_out"],
        "\"Touchdown on the lake bed. Nice landing.\" / \"The skids are down. He's rolling out.\" The successes of runway-landing, at the reveal."),
}
MORE_LINES = {
    "radio-guidance-internal": (["pressurization_is_complete (1)", "pressurization_is_complete (2)", "pressurization_is_complete (3)", "pressurization_is_complete (4)", "were_on_internal_power (1)", "were_on_internal_power (2)"],
        " Round 2 (2026-10-21): \"Pressurization is complete.\" / \"We're on internal power.\""),
    "radio-cutoff": (["ignition_and_cutoff_we_have_a_fire_at_the_base (1)", "ignition_and_cutoff_we_have_a_fire_at_the_base (2)", "engine_shutdown_on_the_pad_the_vehicle_is_damaged (1)", "engine_shutdown_on_the_pad_the_vehicle_is_damaged (2)"],
        " Round 2 (2026-10-21): \"Ignition, and cutoff. We have a fire at the base.\" / \"Engine shutdown on the pad. The vehicle is damaged.\""),
    "radio-max-q": (["were_past_max_q_vehicle_is_stable (1)", "were_past_max_q_vehicle_is_stable (2)", "max_q_everything_looks_good (1)", "max_q_everything_looks_good (2)", "max_q_everything_looks_good (3)", "max_q_everything_looks_good (4)"],
        " Round 2 (2026-10-21): \"We're past Max-Q. Vehicle is stable.\" / \"Max-Q. Everything looks good.\""),
    "radio-trajectory-low": (["upper_stage_is_off_the_path_guidance_has_it_back_on_trajectory (1)", "upper_stage_is_off_the_path_guidance_has_it_back_on_trajectory (2)", "were_a_little_off_in_pitch_correcting_now_looks_good (1)", "were_a_little_off_in_pitch_correcting_now_looks_good (2)", "were_a_little_off_in_pitch_correcting_now_looks_good (3)"],
        " Round 2 (2026-10-21): \"Upper stage is off the path. Guidance has it. Back on trajectory.\" / \"We're a little off in pitch. Correcting now. Looks good.\""),
    "radio-orbit-insertion": (["engine_cutoff_we_have_a_good_orbit", "were_in_orbit_parameters_look_good (1)", "were_in_orbit_parameters_look_good (2)"],
        " Round 2 (2026-10-21): \"Engine cutoff. We have a good orbit.\" / \"We're in orbit. Parameters look good.\""),
    "radio-burn-short": (["engine_cut_out_we_have_a_relight_burn_is_complete (1)", "engine_cut_out_we_have_a_relight_burn_is_complete (2)", "engine_cut_out_we_have_a_relight_burn_is_complete (3)", "we_lost_thrust_for_a_moment_thrust_is_back_good_burn (1)", "we_lost_thrust_for_a_moment_thrust_is_back_good_burn (2)"],
        " Round 2 (2026-10-21): \"Engine cut out. We have a relight. Burn is complete.\" / \"We lost thrust for a moment. Thrust is back. Good burn.\" Also on the glitches of high-orbit-injection, loi, lunar-ascent, mars-orbit-injection, rendezvous-target-approach, tei, tpi, viking-separation, at the reveal."),
    "radio-burn-failed": (["engine_cut_out_we_cant_restart_it (1)", "engine_cut_out_we_cant_restart_it (2)", "the_relight_failed_were_well_short (1)", "the_relight_failed_were_well_short (2)", "the_relight_failed_were_well_short (3)"],
        " Round 2 (2026-10-21): \"Engine cut out. We can't restart it.\" / \"The relight failed. We're well short.\" Also on the failures of high-orbit-injection, insertion, mars-orbit-injection, tpi, at the reveal."),
    "radio-problem": (["flight_we_have_a_caution_light_here_looks_like_were_okay (1)", "flight_we_have_a_caution_light_here_looks_like_were_okay (2)", "flight_we_have_a_caution_light_here_looks_like_were_okay (3)", "flight_we_have_a_caution_light_here_looks_like_were_okay (4)", "weve_got_a_problem_with_the_power_working_it_were_okay_now"],
        " Round 2 (2026-10-21): \"Flight, we have a caution light here. Looks like we're okay.\" / \"We've got a problem with the power. Working it. We're okay now.\" Also on the glitches of earth-departure, luna-sampler, lunar-orbit, lunokhod-deploy, lunokhod-drive, mars-rover, mars-surface, probe-surface, at the reveal."),
    "radio-off-trajectory": (["the_correction_misfired_its_off_course (1)", "the_correction_misfired_its_off_course (2)", "its_going_to_miss_we_cant_correct_it (1)", "its_going_to_miss_we_cant_correct_it (2)"],
        " Round 2 (2026-10-21): \"The correction misfired. It's off course.\" / \"It's going to miss. We can't correct it.\" Also on the failures of jupiter-approach, jupiter-flyby, lunar-flyby, mars-approach, mars-flyby, mercury-flyby, saturn-approach, saturn-flyby, venus-approach, venus-flyby, at the reveal."),
    "radio-pressure": (["oxygen_is_low_were_bringing_them_home_early (1)", "oxygen_is_low_were_bringing_them_home_early (2)", "oxygen_is_low_were_bringing_them_home_early (3)", "flight_we_have_to_end_the_mission_consumables_are_critical (1)", "flight_we_have_to_end_the_mission_consumables_are_critical (2)", "flight_we_have_to_end_the_mission_consumables_are_critical (3)", "flight_we_have_to_end_the_mission_consumables_are_critical (4)", "flight_we_have_to_end_the_mission_consumables_are_critical (5)"],
        " Round 2 (2026-10-21): \"Oxygen is low. We're bringing them home early.\" / \"Flight, we have to end the mission. Consumables are critical.\""),
    "radio-closing-fast": (["no_braking_there_it_is_a_little_close_backing_off (1)", "no_braking_there_it_is_a_little_close_backing_off (2)", "no_braking_there_it_is_a_little_close_backing_off (3)", "no_braking_there_it_is_a_little_close_backing_off (4)", "no_braking_there_it_is_a_little_close_backing_off (5)", "too_fast_brake_brake_okay_its_stopped (1)", "too_fast_brake_brake_okay_its_stopped (2)", "too_fast_brake_brake_okay_its_stopped (3)", "too_fast_brake_brake_okay_its_stopped (4)", "too_fast_brake_brake_okay_its_stopped (5)"],
        " Round 2 (2026-10-21): \"No braking. There it is. A little close, backing off.\" / \"Too fast. Brake, brake. Okay, it's stopped.\""),
    "radio-latches": (["it_bounced_off_closing_in_again_capture (1)", "it_bounced_off_closing_in_again_capture (2)", "no_capture_on_the_first_try_second_try_were_docked (1)", "no_capture_on_the_first_try_second_try_were_docked (2)", "no_capture_on_the_first_try_second_try_were_docked (3)", "no_capture_on_the_first_try_second_try_were_docked (4)"],
        " Round 2 (2026-10-21): \"It bounced off. Closing in again. Capture.\" / \"No capture on the first try. Second try. We're docked.\" Also on the glitches of transposition, at the reveal."),
    "radio-abort-docking": (["no_capture_the_latches_wont_catch (1)", "no_capture_the_latches_wont_catch (2)", "no_capture_the_latches_wont_catch (3)", "we_cant_get_a_dock_calling_it_off (1)", "we_cant_get_a_dock_calling_it_off (2)"],
        " Round 2 (2026-10-21): \"No capture. The latches won't catch.\" / \"We can't get a dock. Calling it off.\" Also on the failures of transposition, at the reveal."),
    "radio-tumbling": (["we_have_a_stuck_thruster_we_cant_stop_the_roll (1)", "we_have_a_stuck_thruster_we_cant_stop_the_roll (2)", "rates_are_climbing_its_spinning_out_of_control (1)", "rates_are_climbing_its_spinning_out_of_control (2)", "rates_are_climbing_its_spinning_out_of_control (3)", "rates_are_climbing_its_spinning_out_of_control (4)"],
        " Round 2 (2026-10-21): \"We have a stuck thruster. We can't stop the roll.\" / \"Rates are climbing. It's spinning out of control.\""),
    "radio-crew-back-in": (["his_visor_is_fogging_bring_him_back_inside (1)", "his_visor_is_fogging_bring_him_back_inside (2)", "were_ending_the_spacewalk_hes_coming_in (1)", "were_ending_the_spacewalk_hes_coming_in (2)", "were_ending_the_spacewalk_hes_coming_in (3)"],
        " Round 2 (2026-10-21): \"His visor is fogging. Bring him back inside.\" / \"We're ending the spacewalk. He's coming in.\" Also on the failures of eva-in, at the reveal."),
    "radio-payload-separation": (["spacecraft_separation_its_on_its_own (1)", "spacecraft_separation_its_on_its_own (2)", "spacecraft_separation_its_on_its_own (3)", "spacecraft_separation_its_on_its_own (4)", "good_separation_the_payload_is_free (1)", "good_separation_the_payload_is_free (2)", "good_separation_the_payload_is_free (3)", "good_separation_the_payload_is_free (4)", "good_separation_the_payload_is_free (5)"],
        " Round 2 (2026-10-21): \"Spacecraft separation. It's on its own.\" / \"Good separation. The payload is free.\""),
    "radio-separation-late": (["one_latch_is_holding_second_command_its_clear (1)", "one_latch_is_holding_second_command_its_clear (2)", "its_hanging_on_there_it_goes_clean_now (1)", "its_hanging_on_there_it_goes_clean_now (2)"],
        " Round 2 (2026-10-21): \"One latch is holding. Second command. It's clear.\" / \"It's hanging on. There it goes. Clean now.\" Also on the glitches of multiprobe-large-release, multiprobe-small-release, probe-release, at the reveal."),
    "radio-separation-failed": (["its_still_attached_we_cant_shake_it_loose (1)", "its_still_attached_we_cant_shake_it_loose (2)", "no_separation_the_pyros_did_not_fire (1)", "no_separation_the_pyros_did_not_fire (2)", "no_separation_the_pyros_did_not_fire (3)", "no_separation_the_pyros_did_not_fire (4)", "no_separation_the_pyros_did_not_fire (5)", "no_separation_the_pyros_did_not_fire (6)"],
        " Round 2 (2026-10-21): \"It's still attached. We can't shake it loose.\" / \"No separation. The pyros did not fire.\" Also on the failures of lander-staging, at the reveal."),
    "radio-attitude-drift": (["its_swinging_too_far_jets_have_it_attitude_is_good (1)", "its_swinging_too_far_jets_have_it_attitude_is_good (2)", "overshoot_on_the_turn_damped_out_holding_steady (1)", "overshoot_on_the_turn_damped_out_holding_steady (2)", "overshoot_on_the_turn_damped_out_holding_steady (3)", "overshoot_on_the_turn_damped_out_holding_steady (4)", "overshoot_on_the_turn_damped_out_holding_steady (5)"],
        " Round 2 (2026-10-21): \"It's swinging too far. Jets have it. Attitude is good.\" / \"Overshoot on the turn. Damped out. Holding steady.\""),
    "radio-roger": (["attitude_is_good_ready_for_retrofire (1)", "attitude_is_good_ready_for_retrofire (2)", "attitude_is_good_ready_for_retrofire (3)", "copy_retro_attitude_is_set (1)", "copy_retro_attitude_is_set (2)"],
        " Round 2 (2026-10-21): \"Attitude is good. Ready for retrofire.\" / \"Copy. Retro attitude is set.\""),
    "radio-blackout": (["entering_blackout (1)", "entering_blackout (2)", "loss_of_signal_reentry_has_begun (1)", "loss_of_signal_reentry_has_begun (2)"],
        " Round 2 (2026-10-21): \"Entering blackout.\" / \"Loss of signal. Reentry has begun.\""),
    "radio-oscillations": (["its_oscillating_damping_out_now (1)", "its_oscillating_damping_out_now (2)", "were_seeing_some_wobble_on_entry_its_steady_again (1)", "were_seeing_some_wobble_on_entry_its_steady_again (2)"],
        " Round 2 (2026-10-21): \"It's oscillating. Damping out now.\" / \"We're seeing some wobble on entry. It's steady again.\""),
    "radio-no-acquisition": (["no_signal_after_blackout (1)", "no_signal_after_blackout (2)", "no_signal_after_blackout (3)", "recovery_has_nothing_on_the_beacon (1)", "recovery_has_nothing_on_the_beacon (2)", "recovery_has_nothing_on_the_beacon (3)"],
        " Round 2 (2026-10-21): \"No signal after blackout.\" / \"Recovery has nothing on the beacon.\""),
    "radio-chute-partial": (["the_main_is_streaming_its_filling_good_chute (1)", "the_main_is_streaming_its_filling_good_chute (2)", "the_main_is_streaming_its_filling_good_chute (3)", "the_main_is_streaming_its_filling_good_chute (4)", "main_is_only_half_open_there_its_fully_open_now (1)", "main_is_only_half_open_there_its_fully_open_now (2)", "main_is_only_half_open_there_its_fully_open_now (3)"],
        " Round 2 (2026-10-21): \"The main is streaming. It's filling. Good chute.\" / \"Main is only half open. There, it's fully open now.\" Also on the glitches of descent-and-splashdown, at the reveal."),
    "radio-main-chute-failed": (["the_main_is_not_opening_its_falling_fast (1)", "the_main_is_not_opening_its_falling_fast (2)", "its_a_streamer_no_main (1)", "its_a_streamer_no_main (2)", "its_a_streamer_no_main (3)", "its_a_streamer_no_main (4)"],
        " Round 2 (2026-10-21): \"The main is not opening. It's falling fast.\" / \"It's a streamer. No main.\""),
    "radio-hard-landing": (["hard_landing_it_tipped_over_everything_checks_out (1)", "hard_landing_it_tipped_over_everything_checks_out (2)", "hard_landing_it_tipped_over_everything_checks_out (3)"],
        " Round 2 (2026-10-21): \"Hard landing. It tipped over. Everything checks out.\" Also on the glitches of runway-landing, at the reveal."),
    "radio-capsule-sinking": (["its_flooding_the_helicopter_cant_lift_it (1)", "its_flooding_the_helicopter_cant_lift_it (2)"],
        " Round 2 (2026-10-21): \"It's flooding. The helicopter can't lift it.\""),
    "radio-recovery-no-contact": (["weve_lost_the_beacon (1)", "weve_lost_the_beacon (2)", "weve_lost_the_beacon (3)"],
        " Round 2 (2026-10-21): \"We've lost the beacon.\""),
    "radio-program-alarm": (["we_have_a_master_alarm_okay_we_can_continue (1)", "we_have_a_master_alarm_okay_we_can_continue (2)", "alarm_on_the_computer_its_the_same_alarm_were_go"],
        " Round 2 (2026-10-21): \"We have a master alarm. Okay, we can continue.\" / \"Alarm on the computer. It's the same alarm. We're go.\""),
    "radio-abort-landing": (["descent_engine_is_out_abort_stage (1)", "descent_engine_is_out_abort_stage (2)", "descent_engine_is_out_abort_stage (3)", "abort_the_descent_ascent_engine_to_orbit (1)", "abort_the_descent_ascent_engine_to_orbit (2)"],
        " Round 2 (2026-10-21): \"Descent engine is out. Abort, stage.\" / \"Abort the descent. Ascent engine to orbit.\""),
    "radio-contact-light": (["low_fuel_sixty_seconds_touchdown (1)", "low_fuel_sixty_seconds_touchdown (2)", "low_fuel_sixty_seconds_touchdown (3)", "fuel_is_getting_low_its_hovering_down (1)", "fuel_is_getting_low_its_hovering_down (2)", "fuel_is_getting_low_its_hovering_down (3)"],
        " Round 2 (2026-10-21): \"Low fuel. Sixty seconds. Touchdown.\" / \"Fuel is getting low. It's hovering. Down.\""),
    "radio-lander-lost": (["no_signal_from_the_lander (1)", "no_signal_from_the_lander (2)", "no_signal_from_the_lander (3)", "the_signal_stopped_short_of_the_ground (1)", "the_signal_stopped_short_of_the_ground (2)", "the_signal_stopped_short_of_the_ground (3)"],
        " Round 2 (2026-10-21): \"No signal from the lander.\" / \"The signal stopped short of the ground.\""),
    "radio-collision": (["the_stage_has_hit_the_payload (1)", "the_stage_has_hit_the_payload (2)"],
        " Round 2 (2026-10-21): \"The stage has hit the payload.\""),
    "radio-probe-silent": (["the_lander_has_gone_quiet (1)", "the_lander_has_gone_quiet (2)", "the_lander_has_gone_quiet (3)", "weve_lost_the_signal_from_the_surface (1)", "weve_lost_the_signal_from_the_surface (2)", "weve_lost_the_signal_from_the_surface (3)"],
        " Round 2 (2026-10-21): \"The lander has gone quiet.\" / \"We've lost the signal from the surface.\""),
}
for _sid, (_files, _said) in MORE_LINES.items():
    SOUNDS[_sid] = (SOUNDS[_sid][0] + _files, SOUNDS[_sid][1] + _said)
SOUNDS.update(ROUND_2)

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
        entry = {"id": sid, "kind": "RADIO", "volume": 0.7, "files": files, "credit": True,
                 "description": said + COMMON}
        said_lines = [re.sub(r" \(\d+\)$", "", src) for src in sources]
        if len(set(said_lines)) < len(said_lines):  # takes of one line: the replay deals lines, not takes
            order = list(dict.fromkeys(said_lines))
            entry["lines"] = [order.index(line) for line in said_lines]
        catalog["sounds"].append(entry)
    with open(CATALOG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        f.write("\n")
    used = {s for srcs, _ in SOUNDS.values() for s in srcs}
    left = [f for f in os.listdir(SRC) if f.endswith(".mp3") and f[:-4] not in used]
    print("unused:", left)


if __name__ == "__main__":
    main()
