"""Converts the prepared program, category and solar-system art (the user's own) into the
client's assets, named by content id so the client can find an image from the pack alone.

    assets/ui/programs/<program-id>.jpg              896 x 576 (shown at up to 620 x 399)
    assets/ui/categories/<category-id>_<AGENCY>.jpg  448 x 288 (shown at 112 x 72)
    assets/ui/solar_system.jpg                       1920 x 832
    assets/ui/configurations/<configuration-id>.jpg  560 x 360 (shown at up to 280 x 180)
    assets/ui/rockets/<rocket-id>.jpg                448 x 288 (shown at 140 x 90 in the VAB's lists)

Add a line to PROGRAMS, CATEGORIES or CONFIGURATIONS when a content pack gains one. The early-era
pack's ids are a subset of the standard pack's, so one set of maps serves both.
Run from the repo root:  python assets-raw/make_content_art.py   (needs Pillow)
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, ".."))
OUT = os.path.join(REPO, "assets", "ui")

# Every program, category and configuration in the standard pack (BASPM's whole tree, 2026-10-08).
PROGRAMS = {
    "sputnik-1": "sputnik.png",
    "explorer-1": "explorer_I.png",
    "pegasus": "pegasus_satellite.png",
    "biosatellite": "bio_satellite.png",
    "orbiting-frog-otolith": "ofo_orbiting_frog_otolith.png",
    "korabl-sputnik-2": "korabl_sputnik_2.png",
    "sputnik-2": "sputnik_2.png",
    "pioneer-4": "pioneer_4.png",
    "zond-5": "zond_5.png",
    "ranger-3": "ranger_3.png",
    "luna-2": "luna_2.png",
    "pioneer-2": "pioneer_2.png",
    "lunar-orbiter": "lunar_orbiter.png",
    "surveyor": "surveyor.png",
    "luna-15": "luna_15.png",
    "luna-3": "luna_3.png",
    "luna-17": "luna_17_lunokhod.png",
    "pioneer-sun-orbiter": "pioneer_6.png",
    "mariner-10": "mariner_10.png",
    "mariner-5": "mariner_5.png",
    "pioneer-venus-orbiter": "pioneer_venus_orbiter.png",
    "venera-15": "venera_15.png",
    "pioneer-venus-multiprobe": "pioneer_venus_multiprobe.png",
    "venera-7": "venera_7.png",
    "venera-lander": "venera_venus_orbiter_w_lander.png",
    "mariner-6": "mariner_6.png",
    "mariner-9": "mariner_9.png",
    "viking": "mars_viking.png",
    "mars-2": "mars_2_orbiter_and_lander.png",
    "mars-m69": "mars_m_69.png",
    "pioneer-10": "pioneer_10.png",
    "pioneer-11": "pioneer_11.png",
    "x-15": "x_15.png",
    "mig-e-154": "okb_mig_e_154.png",
    "mercury": "mercury.png",
    "vostok": "vostok.png",
    "gemini": "gemini.png",
    "circumlunar-gemini-earth-orbit": "circumlunar_gemini_testflight_leo.png",
    "lunar-orbital-gemini-earth-orbit": "lunar_orbital_gemini_testflight_leo.png",
    "gemini-direct-ascent-earth-orbit": "gemini_direct_ascent_testflight_leo.png",
    "voskhod": "voskhod.png",
    "soyuz-7k-l1-earth-orbit": "soyuz_7K_L1.png",
    "soyuz-7k-l3-earth-orbit": "soyuz_7K_L3.png",
    "lk-700-earth-orbit": "lk_700_testflight_leo.png",
    "gemini-eor-earth-orbit": "gemini_eor_testflight_leo.png",
    "circumlunar-gemini": "circumlunar_gemini.png",
    "lunar-orbital-gemini": "lunar_orbital_gemini.png",
    "gemini-direct-ascent": "gemini_direct_ascent.png",
    "apollo": "apollo.png",
    "gemini-eor": "gemini_eor.png",
    "soyuz-7k-l1": "circumlunar_soyuz_7K_L1.png",
    "soyuz-7k-l3": "soyuz_7K_LOK.png",
    "lk-700": "lk_700.png",
    "apollo-earth-orbit": "apollo_testflight_leo.png",
    "soyuz-7k-ok": "soyuz_7K_OK.png",
}

CATEGORIES = {
    "earth-orbit-satellites": "earth_orbiting_satellites",
    "lunar-probes": "lunar_probes",
    "sun-probes": "sun_probes",
    "mercury-probes": "mercury_probes",
    "venus-probes": "venus_probes",
    "mars-probes": "mars_probes",
    "jupiter-probes": "jupiter_probes",
    "saturn-probes": "saturn_probes",
    "space-planes": "space_planes",
    "one-crew-capsules": "one_crew_ballistic_capsule",
    "two-crew-capsules": "two_crew_ballistic_capsule",
    "lunar-expeditions": "short_duration_lunar_expeditions",
    "three-crew-capsules": "three_crew_ballistic_capsule",
}

CONFIGURATIONS = {
    "sputnik-1-orbit": "sputnik_regular.png",
    "sputnik-1-extended": "sputnik_extended.png",
    "explorer-1-orbit": "explorer_i_regular.png",
    "explorer-1-extended": "explorer_i_extended.png",
    "pegasus-orbit": "pegasus_regular.png",
    "pegasus-extended": "pegasus_extended.png",
    "biosatellite-orbit": "biosatellite_regular.png",
    "biosatellite-extended": "biosatellite_extended.png",
    "orbiting-frog-otolith-orbit": "orbiting_frog_otolith_regular.png",
    "orbiting-frog-otolith-extended": "orbiting_frog_otolith_extended.png",
    "korabl-sputnik-2-orbit": "korabl_sputnik_2.png",
    "sputnik-2-orbit": "sputnik_2.png",
    "pioneer-4-flyby": "pioneer_4.png",
    "zond-5-flyby": "zond_5.png",
    "ranger-3-impact": "ranger_3.png",
    "luna-2-impact": "luna_2.png",
    "pioneer-2-orbit": "pioneer_2.png",
    "lunar-orbiter-mapping": "lunar_orbiter.png",
    "surveyor-landing": "surveyor.png",
    "luna-15-sample-return": "luna_15.png",
    "luna-3-flyby": "luna_3.png",
    "luna-17-landing": "luna_17_lunokhod.png",
    "pioneer-sun-orbit": "pioneer_6.png",
    "mariner-10-flyby": "mariner_10.png",
    "mariner-5-flyby": "mariner_5.png",
    "pioneer-venus-orbit": "pioneer_12.png",
    "venera-15-orbit": "venera_15.png",
    "pioneer-venus-multiprobe-entry": "pioneer_13_venus_multiprobe.png",
    "venera-7-landing": "venera_7.png",
    "venera-flyby-and-lander": "venera_11.png",
    "venera-orbiter-and-lander": "venera_10.png",
    "mariner-6-flyby": "mariner_6.png",
    "mariner-9-orbit": "mariner_9.png",
    "viking-orbiter-and-lander": "viking_1.png",
    "mars-2-orbiter-lander-rover": "mars_2_orbiter_and_lander.png",
    "mars-m69-orbit": "mars_m_69.png",
    "pioneer-10-flyby": "pioneer_10.png",
    "pioneer-11-flyby": "pioneer_11.png",
    "x-15-flight-test": "x_15_flight_test.png",
    "x-15-research-flight-1": "x_15_experimental_flight_i.png",
    "x-15-research-flight-2": "x_15_experimental_flight_ii.png",
    "mig-e-154-drop-test": "okb_mig_e_154_drop_test.png",
    # The E-154 is the Soviet X-15 (2026-10-17): its altitude flight has the picture at the edge of space, its speed
    # flight the one streaking hot.
    "mig-e-154-research-flight-1": "okb_mig_e_154_orbital.png",
    "mig-e-154-research-flight-2": "okb_mig_e_154_suborbital.png",
    "mercury-uncrewed-suborbital": "mercury_uncrewed_suborbital.png",
    "mercury-suborbital": "mercury_suborbital.png",
    "mercury-uncrewed-orbital": "mercury_uncrewed_orbital.png",
    "mercury-orbital": "mercury_orbital.png",
    "mercury-orbital-duration-1": "mercury_orbital_extended.png",
    "mercury-joint-orbital": "mercury_joint_launch.png",
    "vostok-uncrewed-suborbital": "vostok_uncrewed_suborbital.png",
    "vostok-suborbital": "vostok_suborbital.png",
    "vostok-uncrewed-orbital": "vostok_uncrewed_orbital.png",
    "vostok-orbital": "vostok_orbital.png",
    "vostok-orbital-duration-1": "vostok_orbital_extended_1.png",
    "vostok-orbital-duration-2": "vostok_orbital_extended_2.png",
    "vostok-joint-orbital": "vostok_joint_orbital.png",
    "gemini-uncrewed-suborbital": "gemini_uncrewed_suborbital.png",
    "gemini-suborbital": "gemini_suborbital.png",
    "gemini-uncrewed-orbital": "gemini_uncrewed_orbital.png",
    "gemini-orbital": "gemini_orbital.png",
    "gemini-spacewalk": "gemini_spacewalk.png",
    "gemini-rendezvous": "gemini_orbital_rendezvous.png",
    "gemini-docking": "gemini_docking.png",
    "gemini-spacewalk-and-rendezvous": "gemini_orbital_rendezvous_and_eva.png",
    "gemini-spacewalk-and-docking": "gemini_eva_and_docking.png",
    "gemini-spacewalk-duration-2": "gemini_spacewalk_extended.png",
    "gemini-rendezvous-duration-2": "gemini_orbital_rendezvous_extended.png",
    "gemini-docking-duration-2": "gemini_docking_extended.png",
    "gemini-spacewalk-and-rendezvous-duration-2": "gemini_orbital_rendezvous_and_eva_extended.png",
    "gemini-spacewalk-and-docking-duration-2": "gemini_eva_and_docking_extended.png",
    "gemini-orbital-duration-3": "gemini_orbital_extended.png",
    "circumlunar-gemini-heat-shield-test": "gemini_heat_shield_test.png",
    "circumlunar-gemini-transtage-test": "gemini_crewed_transtage_and_heat_shield.png",
    "lunar-orbital-gemini-heat-shield-test": "gemini_uncrewed_lunar_propulsion_module_and_heat_shield.png",
    "lunar-orbital-gemini-propulsion-module-test": "gemini_crewed_lunar_propulsion_module_and_heat_shield.png",
    "gemini-direct-ascent-uncrewed-test": "gemini_direct_ascent_uncrewed.png",
    "gemini-direct-ascent-crewed-test": "gemini_direct_ascent_crewed.png",
    "voskhod-uncrewed-suborbital": "voskhod_uncrewed_suborbital.png",
    "voskhod-suborbital": "voskhod_suborbital.png",
    "voskhod-uncrewed-orbital": "voskhod_uncrewed_orbital.png",
    "voskhod-orbital": "voskhod_orbital.png",
    "voskhod-orbital-duration-1": "voskhod_orbital_extended_1.png",
    "voskhod-orbital-duration-2": "voskhod_orbital_extended_2.png",
    "voskhod-orbital-duration-3": "voskhod_orbital_extended_3.png",
    "voskhod-orbital-duration-4": "voskhod_orbital_extended_4.png",
    "voskhod-spacewalk": "voskhod_spacewalk.png",
    "voskhod-joint-orbital": "voskhod_joint_launch.png",
    "soyuz-7k-l1-uncrewed-suborbital": "soyuz_7K_L1_uncrewed_suborbital.png",
    "soyuz-7k-l1-suborbital": "soyuz_7K_L1_suborbital.png",
    "soyuz-7k-l1-uncrewed-orbital": "soyuz_7K_L1_uncrewed_orbital.png",
    "soyuz-7k-l1-orbital": "soyuz_7K_L1_orbital.png",
    "soyuz-7k-l1-orbital-duration-1": "soyuz_7K_L1_orbital_extended_1.png",
    "soyuz-7k-l1-orbital-duration-2": "soyuz_7K_L1_orbital_extended_2.png",
    "soyuz-7k-l3-uncrewed-suborbital": "soyuz_7K_L3_LOK_uncrewed_suborbital.png",
    "soyuz-7k-l3-suborbital": "soyuz_7K_L3_LOK_suborbital.png",
    "soyuz-7k-l3-uncrewed-orbital": "soyuz_7K_L3_LOK_uncrewed_orbital.png",
    "soyuz-7k-l3-orbital": "soyuz_7K_L3_LOK_orbital.png",
    "soyuz-7k-l3-orbital-duration-1": "soyuz_7K_L3_LOK_orbital_extended_1.png",
    "soyuz-7k-l3-orbital-duration-2": "soyuz_7K_L3_LOK_orbital_extended_2.png",
    "soyuz-7k-l3-orbital-duration-3": "soyuz_7K_L3_LOK_orbital_extended_3.png",
    "soyuz-7k-l3-spacewalk": "soyuz_7K_L3_LOK_LK_spacewalk.png",
    "soyuz-7k-l3-joint-orbital": "soyuz_7K_L3_LOK_joint_launch.png",
    "soyuz-7k-l3-automatic-rendezvous": "soyuz_7K_L3_LOK_automatic_rendezvous.png",
    "soyuz-7k-l3-uncrewed-lander-test": "soyuz_7K_L3_LOK_LK_uncrewed_lander_test.png",
    "soyuz-7k-l3-lander-test": "soyuz_7K_L3_LOK_LK_lander_test.png",
    "lk-700-uncrewed-suborbital": "lk_700_uncrewed_suborbital.png",
    "lk-700-suborbital": "lk_700_suborbital.png",
    "lk-700-uncrewed-orbital": "lk_700_uncrewed_orbital.png",
    "lk-700-orbital": "lk_700_orbital.png",
    "lk-700-orbital-duration-1": "lk_700_orbital_extended_1.png",
    "lk-700-orbital-duration-2": "lk_700_orbital_extended_2.png",
    "lk-700-orbital-duration-3": "lk_700_orbital_extended_3.png",
    "lk-700-orbital-duration-4": "lk_700_orbital_extended_4.png",
    "lk-700-orbital-duration-5": "lk_700_orbital_extended_5.png",
    "lk-700-orbital-duration-6": "lk_700_orbital_extended_6.png",
    "lk-700-spacewalk": "lk_700_spacewalk.png",
    "gemini-eor-lander-test": "gemini_eor_lander_test.png",
    "gemini-eor-lander-and-heat-shield-test": "gemini_eor_lander_and_heat_shield_test.png",
    "circumlunar-gemini-transtage-flyby": "circumlunar_gemini_transtage_lunar_flyby.png",
    "circumlunar-gemini-flyby": "circumlunar_gemini_lunar_flyby.png",
    "lunar-orbital-gemini-uncrewed-flyby": "lunar_orbital_gemini_uncrewed_flyby.png",
    "lunar-orbital-gemini-flyby": "lunar_orbital_gemini_circumlunar_flight.png",
    "lunar-orbital-gemini-uncrewed-lunar-orbit": "lunar_orbital_gemini_uncrewed_lunar_orbital.png",
    "lunar-orbital-gemini-lunar-orbit": "lunar_orbital_gemini_lunar_orbital.png",
    "gemini-direct-ascent-uncrewed-flyby": "gemini_direct_ascent_uncrewed_circumlunar.png",
    "gemini-direct-ascent-flyby": "gemini_direct_ascent_circumlunar.png",
    "gemini-direct-ascent-uncrewed-lunar-orbit": "gemini_direct_ascent_uncrewed_lunar_orbital.png",
    "gemini-direct-ascent-lunar-orbit": "gemini_direct_ascent_lunar_orbital.png",
    "gemini-direct-ascent-landing": "gemini_direct_ascent_lunar_landing.png",
    "apollo-uncrewed-flyby": "apollo_uncrewed_circumlunar.png",
    "apollo-flyby": "apollo_circumlunar.png",
    "apollo-uncrewed-lunar-orbit": "apollo_uncrewed_lunar_orbit.png",
    "apollo-lunar-orbit": "apollo_lunar_orbit.png",
    "apollo-lander-test": "apollo_lunar_module_test.png",
    "apollo-landing": "apollo_lunar_landing_type_g_h.png",
    "apollo-landing-with-rover": "apollo_lunar_landing_type_j.png",
    "gemini-eor-flyby": "gemini_eor_circumlunar.png",
    "gemini-eor-lunar-orbit": "gemini_eor_lunar_orbital.png",
    "gemini-eor-lander-test-lunar-orbit": "gemini_eor_langley.png",
    "gemini-eor-landing": "gemini_eor_lunar_landing.png",
    "soyuz-7k-l1-uncrewed-flyby": "soyuz_7K_L1_uncrewed_circumlunar_flyby.png",
    "soyuz-7k-l1-flyby": "soyuz_7K_L1_circumlunar_flyby.png",
    "soyuz-7k-l3-uncrewed-flyby": "soyuz_7K_LOK_uncrewed_circumlunar.png",
    "soyuz-7k-l3-flyby": "soyuz_7K_LOK_circumlunar.png",
    "soyuz-7k-l3-uncrewed-lunar-orbit": "soyuz_7K_LOK_uncrewed_lunar_orbital.png",
    "soyuz-7k-l3-lunar-orbit": "soyuz_7K_LOK_lunar_orbital.png",
    "soyuz-7k-l3-lander-test-lunar-orbit": "soyuz_7K_LOK_lander_test.png",
    "soyuz-7k-l3-landing": "soyuz_7K_LOK_LOR.png",
    "lk-700-uncrewed-flyby": "lk_700_uncrewed_circumlunar.png",
    "lk-700-flyby": "lk_700_circumlunar.png",
    "lk-700-uncrewed-lunar-orbit": "lk_700_uncrewed_lunar_orbital.png",
    "lk-700-lunar-orbit": "lk_700_lunar_orbital.png",
    "lk-700-landing": "lk_700_lunar_landing.png",
    "apollo-uncrewed-suborbital": "apollo_uncrewed_suborbital.png",
    "apollo-suborbital": "apollo_suborbital_type_c.png",
    "apollo-uncrewed-orbital": "apollo_uncrewed_orbital_type_a.png",
    "apollo-orbital": "apollo_orbital_type_c.png",
    "apollo-uncrewed-lunar-module-test": "apollo_uncrewed_orbital_lem_type_b.png",
    "apollo-lunar-module-test-low-orbit": "apollo_lunar_module_test_with_eva_type_d.png",
    "apollo-lunar-module-test-high-orbit": "apollo_lunar_module_and_CM_test_with_eva_type_e.png",
    "apollo-orbital-duration-1": "apollo_orbital_extended_1.png",
    "apollo-orbital-duration-2": "apollo_orbital_extended_2.png",
    "apollo-orbital-duration-3": "apollo_orbital_extended_3.png",
    "apollo-orbital-duration-4": "apollo_orbital_extended_4.png",
    "apollo-spacewalk-duration-1": "apollo_orbital_eva_extended_1.png",
    "apollo-spacewalk-duration-2": "apollo_orbital_eva_extended_2.png",
    "apollo-spacewalk-duration-3": "apollo_orbital_eva_extended_3.png",
    "apollo-spacewalk-duration-4": "apollo_orbital_eva_extended_4.png",
    "apollo-rendezvous": "apollo_orbital_rendezvous.png",
    "soyuz-7k-ok-uncrewed-suborbital": "soyuz_7K_OK_uncrewed_suborbital.png",
    "soyuz-7k-ok-suborbital": "soyuz_7K_OK_suborbital.png",
    "soyuz-7k-ok-uncrewed-orbital": "soyuz_7K_OK_uncrewed_orbital.png",
    "soyuz-7k-ok-orbital": "soyuz_7K_OK_orbital.png",
    "soyuz-7k-ok-orbital-duration-1": "soyuz_7K_OK_orbital_extended_1.png",
    "soyuz-7k-ok-orbital-duration-2": "soyuz_7K_OK_orbital_extended_2.png",
    "soyuz-7k-ok-orbital-duration-3": "soyuz_7K_OK_orbital_extended_3.png",
    "soyuz-7k-ok-orbital-duration-4": "soyuz_7K_OK_orbital_extended_4.png",
    "soyuz-7k-ok-orbital-duration-5": "soyuz_7K_OK_orbital_extended_5.png",
    "soyuz-7k-ok-orbital-duration-6": "soyuz_7K_OK_orbital_extended_6.png",
    "soyuz-7k-ok-spacewalk": "soyuz_7K_OK_orbital_eva.png",
    "soyuz-7k-ok-joint-orbital": "soyuz_7K_OK_joint_launch_orbital.png",
    "soyuz-7k-ok-automatic-rendezvous": "soyuz_7K_OK_automatic_rendezvous.png",
    "soyuz-7k-ok-uncrewed-docking": "soyuz_7K_OK_uncrewed_automatic_docking.png",
    "soyuz-7k-ok-crewed-docking": "soyuz_7K_OK_crewed_automatic_docking.png",
    "soyuz-7k-ok-crew-transfer": "soyuz_7K_OK_automated_docking_w_crew_transfer.png",
}


# Every rocket in the standard pack; an image not drawn yet is skipped.
ROCKETS = {
    "r7-sputnik": "r7_sputnik.png",
    "r7-vostok": "r7_vostok.png",
    "r7-luna": "r7_luna.png",
    "r7-voskhod": "r7_voskhod.png",
    "r7-molniya": "r7_molnya.png",
    "r7-soyuz": "r7_soyuz.png",
    "proton": "proton_k.png",
    "proton-k-block-d": "proton_kd.png",
    "n1": "n1.png",
    "ur-700": "ur_700.png",
    "jupiter-c": "jupiter_c.png",
    "redstone": "redstone.png",
    "juno-2": "juno_ii.png",
    "thor-able": "thor_able.png",
    "atlas": "atlas.png",
    "atlas-agena": "atlas_agena.png",
    "atlas-centaur": "atlas_centaur.png",
    "titan-2": "titan_ii.png",
    "titan-3c": "titan_iiic.png",
    "titan-3e-centaur": "titan_iiie_centaur.png",
    "saturn-1": "saturn_i.png",
    "saturn-1b": "saturn_ib.png",
    "saturn-c3b": "saturn_c3.png",
    "saturn-v": "saturn_v.png",
}


def save(src, dst, size):
    image = Image.open(src).convert("RGB")
    if size is not None and image.size != size:
        image = image.resize(size, Image.LANCZOS)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    image.save(dst, quality=88)


def main():
    for program, file in PROGRAMS.items():
        save(os.path.join(HERE, "Programs", file), os.path.join(OUT, "programs", program + ".jpg"), (896, 576))
    for category, stem in CATEGORIES.items():
        for agency in ("NASA", "SOVIET"):
            src = os.path.join(HERE, "Program Categories", stem + "_" + agency + ".png")
            if os.path.exists(src):
                save(src, os.path.join(OUT, "categories", category + "_" + agency + ".jpg"), (448, 288))
    save(os.path.join(HERE, "backgrounds", "solar_system.png"), os.path.join(OUT, "solar_system.jpg"), (1920, 832))
    for configuration, file in CONFIGURATIONS.items():
        save(os.path.join(HERE, "Mission Configuration", file), os.path.join(OUT, "configurations", configuration + ".jpg"),
             (560, 360))
    drawn = 0
    for rocket, file in ROCKETS.items():
        src = os.path.join(HERE, "Rocket Programs", file)
        if os.path.exists(src):
            save(src, os.path.join(OUT, "rockets", rocket + ".jpg"), (448, 288))
            drawn += 1
    print("rockets:", drawn, "of", len(ROCKETS), "drawn")
    print("wrote", len(PROGRAMS), "programs,", len(CATEGORIES), "categories,", len(CONFIGURATIONS),
          "configurations and the solar system to", OUT)


if __name__ == "__main__":
    main()
