# Usage: python3 tools/make_lang.py src/main/resources/assets/contagion/lang
"""Writes en_us.json and fr_fr.json for Contagion. Each entry: key -> (English, French)."""
import json
import os
import sys

out = sys.argv[1]
T = {}


def t(key, en, fr):
    assert key not in T, key
    T[key] = (en, fr)


# ---------------------------------------------------------------- items & blocks
for key, en, fr in [
    ("syringe", "Syringe", "Seringue"),
    ("blood_sample", "Blood Sample", "Échantillon de sang"),
    ("petri_dish", "Petri Dish", "Boîte de Petri"),
    ("culture", "Culture Dish", "Boîte de culture"),
    ("mortar", "Mortar and Pestle", "Mortier et pilon"),
    ("prepared_reagent", "Prepared Reagent", "Réactif préparé"),
    ("empty_vial", "Empty Vial", "Fiole vide"),
    ("serum", "Experimental Serum", "Sérum expérimental"),
    ("thermometer", "Thermometer", "Thermomètre"),
    ("research_journal", "Research Journal", "Journal de recherche"),
    ("face_mask", "Face Mask", "Masque"),
    ("microscope", "Microscope", "Microscope"),
    ("beaker", "Beaker", "Bécher"),
]:
    t("item.contagion." + key, en, fr)
t("block.contagion.microscope", "Microscope", "Microscope")
t("block.contagion.beaker", "Beaker", "Bécher")
t("death.attack.contagion.infection", "%1$s succumbed to the infection", "%1$s a succombé à l'infection")
t("death.attack.contagion.infection.player", "%1$s succumbed to the infection while fighting %2$s", "%1$s a succombé à l'infection en combattant %2$s")

# ---------------------------------------------------------------- static item tooltips
for name, l1, l2 in [
    ("syringe", ("Use on someone to draw blood", "Utiliser sur quelqu'un : prise de sang"), ("Sneak + use: draw your own", "Accroupi + utiliser : votre propre sang")),
    ("petri_dish", ("Blood sample in off-hand + use", "Échantillon en main secondaire + utiliser"), ("to start a culture", "pour lancer une culture")),
    ("mortar", ("Reagent in off-hand + use", "Réactif en main secondaire + utiliser"), ("to grind it", "pour le broyer")),
    ("empty_vial", ("Reagent in off-hand + use", "Réactif en main secondaire + utiliser"), ("to start a mixture", "pour commencer un mélange")),
    ("thermometer", ("Hold use to take your temperature", "Maintenir utiliser pour prendre votre température"), ("Hold use on someone to take theirs", "Maintenir sur quelqu'un pour la sienne")),
    ("research_journal", ("Everyone's shared research notes", "Les notes de recherche de tous"), ("Use to read", "Utiliser pour lire")),
    ("face_mask", ("Wear on your head", "Se porte sur la tête"), ("Greatly reduces airborne spread", "Réduit fortement la transmission par l'air")),
    ("microscope", ("Analyzes blood samples", "Analyse les échantillons de sang"), ("and tests serums on cultures", "et teste les sérums sur des cultures")),
    ("beaker", ("Boils reagents into extracts", "Fait bouillir les réactifs en extraits"), ("Needs water and heat below", "Nécessite de l'eau et une source de chaleur dessous")),
]:
    t(f"contagion.lore.{name}.1", *l1)
    t(f"contagion.lore.{name}.2", *l2)

t("contagion.lore.sample.donor", "Donor: %s", "Donneur : %s")
t("contagion.lore.sample.day", "Drawn on day %s (keeps %s days)", "Prélevé le jour %s (se conserve %s jours)")
t("contagion.lore.sample.use1", "Place it in a Microscope to analyze", "À placer dans un Microscope pour l'analyser")
t("contagion.lore.sample.use2", "Use with a Petri Dish to grow a culture", "S'utilise avec une Boîte de Petri pour une culture")
t("contagion.lore.sample.use3", "Sneak + use on someone to transfuse", "Accroupi + utiliser sur quelqu'un : transfusion")
t("contagion.lore.culture.donor", "Blood of %s (day %s)", "Sang de %s (jour %s)")
t("contagion.lore.culture.growing", "Growing...", "En croissance...")
t("contagion.lore.culture.colonized", "Colonies have grown", "Des colonies se sont développées")
t("contagion.lore.culture.sterile", "Nothing grew", "Rien n'a poussé")
t("contagion.lore.culture.dead", "The culture has died", "La culture est morte")
t("contagion.lore.culture.use", "Place it in a Microscope with a serum to test it", "À placer dans un Microscope avec un sérum pour le tester")
t("contagion.lore.prepared", "Can be mixed into a serum", "Peut être ajouté à un sérum")
t("contagion.lore.serum.sealed", "Sealed experimental serum", "Sérum expérimental scellé")
t("contagion.lore.serum.unsealed", "Unsealed mixture (%s/%s)", "Mélange non scellé (%s/%s)")
t("contagion.lore.serum.sealed_use1", "Use twice to inject yourself, or use on someone", "Utiliser deux fois pour vous l'injecter, ou sur quelqu'un")
t("contagion.lore.serum.sealed_use2", "Or test it on a culture with a Microscope", "Ou testez-le sur une culture au Microscope")
t("contagion.lore.serum.unsealed_use1", "Reagent in off-hand + use to add it", "Réactif en main secondaire + utiliser pour l'ajouter")
t("contagion.lore.serum.unsealed_use2", "Sneak + use to seal", "Accroupi + utiliser pour sceller")

# ---------------------------------------------------------------- reagents, categories, forms
for key, en, fr in [
    ("redstone", "Redstone Dust", "Poudre de redstone"), ("glowstone_dust", "Glowstone Dust", "Poudre lumineuse"),
    ("gunpowder", "Gunpowder", "Poudre à canon"), ("amethyst_shard", "Amethyst Shard", "Éclat d'améthyste"),
    ("sugar", "Sugar", "Sucre"), ("sweet_berries", "Sweet Berries", "Baies sucrées"),
    ("glow_berries", "Glow Berries", "Baies lumineuses"), ("cocoa_beans", "Cocoa Beans", "Fèves de cacao"),
    ("brown_mushroom", "Brown Mushroom", "Champignon brun"), ("red_mushroom", "Red Mushroom", "Champignon rouge"),
    ("crimson_fungus", "Crimson Fungus", "Champignon carmin"), ("warped_fungus", "Warped Fungus", "Champignon biscornu"),
    ("spider_eye", "Spider Eye", "Œil d'araignée"), ("rabbit_foot", "Rabbit's Foot", "Patte de lapin"),
    ("slime_ball", "Slimeball", "Boule de slime"), ("honeycomb", "Honeycomb", "Rayon de miel"),
    ("dried_kelp", "Dried Kelp", "Algue séchée"), ("pufferfish", "Pufferfish", "Poisson-globe"),
    ("ink_sac", "Ink Sac", "Poche d'encre"), ("prismarine_crystals", "Prismarine Crystals", "Cristaux de prismarine"),
    ("blaze_powder", "Blaze Powder", "Poudre de blaze"), ("magma_cream", "Magma Cream", "Crème de magma"),
    ("nether_wart", "Nether Wart", "Verrue du Nether"), ("ghast_tear", "Ghast Tear", "Larme de ghast"),
]:
    t("contagion.reagent." + key, en, fr)

for key, en, fr in [("mineral", "Mineral", "Minéral"), ("botanical", "Plant-based", "Végétal"), ("fungal", "Fungal", "Fongique"),
                    ("animal", "Animal-derived", "Animal"), ("aquatic", "Aquatic", "Aquatique"), ("nether", "Nether-born", "Du Nether")]:
    t("contagion.category." + key, en, fr)

t("contagion.form.raw", "raw", "brut")
t("contagion.form.ground", "ground", "broyé")
t("contagion.form.extract", "boiled into an extract", "bouilli en extrait")
t("contagion.ingredient.raw", "%s", "%s")
t("contagion.ingredient.ground", "Ground %s", "%s (broyé)")
t("contagion.ingredient.extract", "%s Extract", "Extrait de %s")
t("contagion.list", "[%s]", "[%s]")
t("contagion.join", "%s, %s", "%s, %s")
t("contagion.header", "=== %s ===", "=== %s ===")

# ---------------------------------------------------------------- strain naming
t("contagion.strain.name", "Strain %1$s \"%2$s %3$s\"", "Souche %1$s « %3$s %2$s »")
adjectives = [("Grey", "grise"), ("Weeping", "suintante"), ("Crimson", "cramoisie"), ("Hollow", "creuse"), ("Silent", "silencieuse"),
              ("Pale", "pâle"), ("Black", "noire"), ("Creeping", "rampante"), ("Rotting", "putride"), ("Burning", "brûlante"),
              ("Withering", "flétrissante"), ("Ashen", "cendrée")]
nouns = [("Fever", "Fièvre"), ("Rot", "Pourriture"), ("Cough", "Toux"), ("Blight", "Flétrissure"), ("Plague", "Peste"), ("Sickness", "Maladie"),
         ("Wasting", "Consomption"), ("Pox", "Vérole"), ("Rash", "Éruption"), ("Shakes", "Tremblante"), ("Bloom", "Efflorescence"), ("Languor", "Langueur")]
for i, (en, fr) in enumerate(adjectives):
    t(f"contagion.strain.adj.{i}", en, fr)
for i, (en, fr) in enumerate(nouns):
    t(f"contagion.strain.noun.{i}", en, fr)
t("contagion.profile.respiratory", "respiratory", "respiratoire")
t("contagion.profile.neurological", "neurological", "neurologique")
t("contagion.profile.hemorrhagic", "hemorrhagic", "hémorragique")
for i, (en, fr) in enumerate([("none", "nulle"), ("trace", "infime"), ("low", "faible"), ("moderate", "modérée"), ("high", "élevée"), ("extreme", "extrême")]):
    t(f"contagion.load.{i}", en, fr)

# ---------------------------------------------------------------- symptoms & stages
for key, en, fr in [
    ("fever1", "You feel feverish.", "Vous vous sentez fiévreux."),
    ("fever2", "You're burning up...", "Vous êtes brûlant de fièvre..."),
    ("chills", "You can't stop shivering.", "Vous n'arrêtez pas de frissonner."),
    ("fatigue1", "You feel unusually tired.", "Vous vous sentez anormalement fatigué."),
    ("fatigue2", "Every movement takes enormous effort.", "Chaque mouvement demande un effort énorme."),
    ("breathless", "You can't catch your breath.", "Vous n'arrivez pas à reprendre votre souffle."),
    ("dizzy", "The world starts spinning.", "Le monde se met à tourner."),
    ("darkness", "Your vision dims.", "Votre vue se trouble."),
    ("faint", "You nearly pass out.", "Vous manquez de vous évanouir."),
    ("sneeze", "*achoo*", "*atchoum*"),
    ("cough", "*cough cough*", "*kof kof*"),
    ("vomit", "You throw up.", "Vous vomissez."),
    ("hallucination", "Did you hear that?", "Vous avez entendu ça ?"),
    ("bleed1", "Your nose is bleeding.", "Vous saignez du nez."),
    ("bleed2", "You're bleeding and it won't stop.", "Vous saignez et ça ne s'arrête pas."),
]:
    t("contagion.symptom." + key, en, fr)
for key, en, fr in [
    ("mild_respiratory", "Your throat feels scratchy.", "Vous avez la gorge qui gratte."),
    ("mild_neurological", "A dull headache settles behind your eyes.", "Un mal de tête sourd s'installe derrière vos yeux."),
    ("mild_hemorrhagic", "You notice bruises you don't remember getting.", "Vous remarquez des bleus sans savoir d'où ils viennent."),
    ("moderate", "You feel properly sick now. This isn't going away on its own.", "Vous êtes vraiment malade. Ça ne passera pas tout seul."),
    ("severe", "Something is very wrong. Your body is starting to fail.", "Quelque chose ne va vraiment pas. Votre corps commence à lâcher."),
    ("terminal", "You can feel your life slipping away...", "Vous sentez la vie vous échapper..."),
    ("relief_fading", "The relief is fading. The symptoms are coming back.", "Le soulagement s'estompe. Les symptômes reviennent."),
]:
    t("contagion.stage." + key, en, fr)

# ---------------------------------------------------------------- thermometer & progress
t("contagion.thermometer.you", "You:", "Vous :")
t("contagion.thermometer.other", "%s:", "%s :")
for key, en, fr in [("normal", "(normal)", "(normale)"), ("elevated", "(slightly elevated)", "(légèrement élevée)"), ("fever", "(fever)", "(fièvre)"),
                    ("high", "(high fever)", "(forte fièvre)"), ("danger", "(dangerously high)", "(dangereusement élevée)")]:
    t("contagion.thermometer." + key, en, fr)
t("contagion.progress.analysis", "Analyzing", "Analyse")
t("contagion.progress.culture", "Testing", "Test")
t("contagion.progress.boil", "Boiling", "Ébullition")
t("contagion.progress.growing", "Growing", "Croissance")
t("contagion.progress.left", "%ss left", "encore %s s")

# ---------------------------------------------------------------- hints (action bar)
for key, en, fr in [
    ("syringe", "Use the syringe on someone to draw their blood, or sneak + use to draw your own.", "Utilisez la seringue sur quelqu'un pour une prise de sang, ou accroupi + utiliser pour la vôtre."),
    ("drew_blood", "You draw a blood sample from %s.", "Vous prélevez un échantillon de sang sur %s."),
    ("blood_drawn_by", "%s draws some of your blood.", "%s vous fait une prise de sang."),
    ("drew_own", "You draw a sample of your own blood.", "Vous prélevez votre propre sang."),
    ("vein_recovering", "%s's veins need to recover first (about %s min).", "Les veines de %s doivent d'abord récupérer (environ %s min)."),
    ("too_weak", "You're too weak to draw blood safely.", "Vous êtes trop faible pour faire une prise de sang."),
    ("confirm_inject", "Inject yourself with this serum? Use again to confirm.", "Vous injecter ce sérum ? Utilisez à nouveau pour confirmer."),
    ("sealed_serum", "Sealed serum: %s. Use twice to inject yourself.", "Sérum scellé : %s. Utilisez deux fois pour vous l'injecter."),
    ("dish_need_sample", "Hold a blood sample in your off-hand to start a culture.", "Tenez un échantillon de sang en main secondaire pour lancer une culture."),
    ("inoculated", "You smear %s's blood on the dish. It needs about %s minutes to grow.", "Vous étalez le sang de %s dans la boîte. Il faut environ %s minutes de croissance."),
    ("culture_ready", "Colonies have grown. Test a serum on it with a Microscope.", "Des colonies ont poussé. Testez un sérum dessus avec un Microscope."),
    ("culture_sterile", "Nothing grew in this dish.", "Rien n'a poussé dans cette boîte."),
    ("culture_dead", "This culture has died.", "Cette culture est morte."),
    ("mortar_need_reagent", "Hold a reagent in your off-hand to grind it.", "Tenez un réactif en main secondaire pour le broyer."),
    ("mortar_only_raw", "Only raw reagents can be ground.", "Seuls les réactifs bruts peuvent être broyés."),
    ("ground", "You grind it into %s.", "Vous le broyez : %s."),
    ("vial_need_reagent", "Hold a reagent in your off-hand to add it to the vial.", "Tenez un réactif en main secondaire pour l'ajouter à la fiole."),
    ("mixture_started", "Added %s to a new mixture.", "%s ajouté à un nouveau mélange."),
    ("mixture_need_reagent", "Hold a reagent in your off-hand to add it, or sneak + use to seal the mixture.", "Tenez un réactif en main secondaire pour l'ajouter, ou accroupi + utiliser pour sceller."),
    ("mixture_full", "The vial is full. Sneak + use to seal it.", "La fiole est pleine. Accroupi + utiliser pour la sceller."),
    ("mixture_duplicate", "This mixture already contains %s.", "Ce mélange contient déjà %s."),
    ("mixture_added", "Added %s (%s/%s).", "%s ajouté (%s/%s)."),
    ("sealed", "Serum sealed. Test it on a culture, or inject it... at your own risk.", "Sérum scellé. Testez-le sur une culture, ou injectez-le... à vos risques."),
]:
    t("contagion.hint." + key, en, fr)

# ---------------------------------------------------------------- microscope & culture tests
for key, en, fr in [
    ("start_analysis", "You prepare a slide of %s's blood. Analysis will take about %s seconds.", "Vous préparez une lame du sang de %s. L'analyse prendra environ %s secondes."),
    ("start_culture", "You expose %s's culture to the serum. Check back in about %s seconds.", "Vous exposez la culture de %s au sérum. Revenez dans environ %s secondes."),
    ("seal_first", "Seal the serum first (sneak + use) before testing it.", "Scellez d'abord le sérum (accroupi + utiliser) avant de le tester."),
    ("need_culture", "To test this serum, hold a grown culture (Petri Dish) in your off-hand.", "Pour tester ce sérum, tenez une culture développée (Boîte de Petri) en main secondaire."),
    ("culture_growing", "That culture hasn't finished growing yet.", "Cette culture n'a pas fini de pousser."),
    ("culture_sterile", "Nothing grew in that dish. There's nothing to test on.", "Rien n'a poussé dans cette boîte. Rien à tester."),
    ("culture_dead", "That culture is dead.", "Cette culture est morte."),
    ("help", "Microscope: use with a blood sample to analyze it, or with a sealed serum (grown culture in off-hand) to test the serum.", "Microscope : utilisez-le avec un échantillon de sang pour l'analyser, ou avec un sérum scellé (culture en main secondaire) pour le tester."),
    ("done_notify", "The microscope has finished. Look through it to read the result.", "Le microscope a terminé. Regardez dedans pour lire le résultat."),
    ("analysis_header", "Blood analysis: %s (drawn day %s)", "Analyse de sang : %s (prélevé le jour %s)"),
    ("culture_header", "Culture test: %s", "Test sur culture : %s"),
    ("degraded", "The sample has degraded too much to read. Blood keeps about %s days.", "L'échantillon est trop dégradé pour être lu. Le sang se conserve environ %s jours."),
    ("antibodies", "Antibodies against the pathogen found. The donor is immune.", "Anticorps contre le pathogène détectés. Le donneur est immunisé."),
    ("negative", "No pathogen detected.", "Aucun pathogène détecté."),
    ("negative_note", "(Very early infections can be missed. If in doubt, test again later.)", "(Une infection très récente peut passer inaperçue. Dans le doute, refaites un test plus tard.)"),
    ("positive", "PATHOGEN DETECTED. Viral load: %s.", "PATHOGÈNE DÉTECTÉ. Charge virale : %s."),
    ("identified", "Identified as %s, a %s pathogen.", "Identifié : %s, un pathogène %s."),
    ("identified2", "It looks like it takes a combination of three active compounds to neutralize it.", "Il semble qu'il faille une combinaison de trois composés actifs pour le neutraliser."),
    ("nothing_new_donor", "Nothing new compared to the last slide of %s's blood from that day. Try a fresh sample tomorrow, or another donor.", "Rien de nouveau par rapport à la dernière lame du sang de %s de ce jour. Réessayez demain, ou avec un autre donneur."),
    ("insight", "New insight: %s", "Nouvelle découverte : %s"),
    ("no_insight", "No new insights from this slide. A sicker donor gives clearer results.", "Rien de nouveau sur cette lame. Un donneur plus malade donne des résultats plus nets."),
    ("recorded", "(Recorded in the Research Journal.)", "(Consigné dans le Journal de recherche.)"),
    ("culture_failed", "The test failed.", "Le test a échoué."),
]:
    t("contagion.microscope." + key, en, fr)
for i, (en, fr) in enumerate([
    ("No effect. The culture keeps growing.", "Aucun effet. La culture continue de pousser."),
    ("Growth slowed somewhat.", "La croissance ralentit un peu."),
    ("Growth strongly inhibited, but the culture survives.", "Croissance fortement freinée, mais la culture survit."),
    ("The culture is completely destroyed!", "La culture est entièrement détruite !"),
]):
    t(f"contagion.culture.efficacy.{i}", en, fr)
t("contagion.culture.trace", "There's a faint, unusual reaction at the edges of the dish...", "Une réaction faible et inhabituelle apparaît sur les bords de la boîte...")
for i, (en, fr) in enumerate([("Healthy tissue sample: unharmed.", "Tissu sain témoin : intact."),
                              ("Healthy tissue sample: noticeable cell damage.", "Tissu sain témoin : lésions cellulaires visibles."),
                              ("Healthy tissue sample: severe necrosis!", "Tissu sain témoin : nécrose sévère !")]):
    t(f"contagion.culture.tissue.{i}", en, fr)

# ---------------------------------------------------------------- facts
t("contagion.fact.category", "The pathogen's membrane breaks down when exposed to %s compounds. One active agent is probably of that kind.",
  "La membrane du pathogène se dégrade au contact de composés de type %s. L'un des agents actifs en fait probablement partie.")
t("contagion.fact.form", "The %s active agent only works when %s.", "L'agent actif de type %s ne fonctionne que %s.")
t("contagion.fact.toxic", "%s destroyed healthy cells in the sample. It is probably toxic to patients.", "%s a détruit des cellules saines de l'échantillon. Probablement toxique pour les patients.")
t("contagion.fact.inert", "%s showed no interaction with the pathogen at all.", "%s n'a montré aucune interaction avec le pathogène.")

# ---------------------------------------------------------------- lab notebook entries
t("contagion.log.analysis", "Day %1$s - Analysis of %2$s's blood: %3$s", "Jour %1$s - Analyse du sang de %2$s : %3$s")
t("contagion.log.culture", "Day %1$s - Test of %2$s on %3$s's culture: %4$s", "Jour %1$s - Test de %2$s sur la culture de %3$s : %4$s")
t("contagion.log.inject", "Day %1$s - Injected %2$s into %3$s: %4$s", "Jour %1$s - %2$s injecté à %3$s : %4$s")
t("contagion.log.inject_toxic", "Day %1$s - Injected %2$s into %3$s: %4$s (side effects!)", "Jour %1$s - %2$s injecté à %3$s : %4$s (effets secondaires !)")
for key, en, fr in [("too_old", "sample too old, no result", "échantillon trop vieux, aucun résultat"),
                    ("antibodies", "negative, antibodies present", "négatif, anticorps présents"),
                    ("negative", "negative", "négatif"),
                    ("positive", "POSITIVE (%s viral load)", "POSITIF (charge virale %s)"),
                    ("positive_insight", "POSITIVE (%s viral load); new insight recorded", "POSITIF (charge virale %s) ; nouvelle découverte notée"),
                    ("no_result", "no result", "aucun résultat"),
                    ("culture", "%s; tissue %s", "%s ; tissu %s"),
                    ("culture_trace", "%s, faint unusual reaction; tissue %s", "%s, réaction faible inhabituelle ; tissu %s")]:
    t("contagion.log.result." + key, en, fr)
for i, (en, fr) in enumerate([("no effect", "aucun effet"), ("growth slowed", "croissance ralentie"), ("growth strongly inhibited", "croissance fortement freinée"),
                              ("CULTURE DESTROYED", "CULTURE DÉTRUITE")]):
    t(f"contagion.log.efficacy.{i}", en, fr)
for i, (en, fr) in enumerate([("unharmed", "intact"), ("damaged", "endommagé"), ("necrotic", "nécrosé")]):
    t(f"contagion.log.tissue.{i}", en, fr)
for key, en, fr in [("cured", "CURED", "GUÉRI"), ("clear", "clear improvement", "nette amélioration"), ("slight", "slight improvement", "légère amélioration"),
                    ("faint", "barely any improvement", "amélioration à peine perceptible"), ("none", "no improvement", "aucune amélioration"),
                    ("healthy", "patient was not infected", "le patient n'était pas infecté")]:
    t("contagion.log.verdict." + key, en, fr)

# ---------------------------------------------------------------- injections & transfusions
for key, en, fr in [
    ("other", "You inject %s with the serum.", "Vous injectez le sérum à %s."),
    ("observed", "%s: %s.", "%s : %s."),
    ("cured", "Within minutes the fever breaks. You can feel your body fighting the infection off. You are cured, and you won't catch it again.",
     "En quelques minutes, la fièvre tombe. Vous sentez votre corps repousser l'infection. Vous êtes guéri, et vous ne l'attraperez plus."),
    ("relief_clear", "The symptoms ease noticeably. It's working... partly.", "Les symptômes s'atténuent nettement. Ça marche... en partie."),
    ("relief_slight", "You feel slightly better.", "Vous vous sentez un peu mieux."),
    ("relief_faint", "Maybe you feel a tiny bit better. Hard to tell.", "Peut-être un tout petit mieux. Difficile à dire."),
    ("weaker", "The relief feels weaker than last time.", "Le soulagement semble plus faible que la dernière fois."),
    ("nothing", "Nothing seems to happen.", "Rien ne semble se passer."),
    ("healthy", "You don't feel anything in particular.", "Vous ne ressentez rien de particulier."),
    ("toxic1", "Your stomach turns. Something in that serum didn't agree with you.", "Votre estomac se retourne. Quelque chose dans ce sérum ne passe pas."),
    ("toxic2", "Your veins burn. That was a bad idea.", "Vos veines brûlent. C'était une mauvaise idée."),
    ("toxic3", "Your body convulses violently!", "Votre corps est pris de violentes convulsions !"),
]:
    t("contagion.inject." + key, en, fr)
t("contagion.title.cured", "Cured", "Guéri")
t("contagion.title.cured_sub", "The infection is gone. You are immune.", "L'infection a disparu. Vous êtes immunisé.")
t("contagion.transfuse.done", "You transfuse %s's blood into %s.", "Vous transfusez le sang de %s à %s.")
t("contagion.transfuse.felt", "%s injects something into your arm.", "%s vous injecte quelque chose dans le bras.")
t("contagion.transfuse.antibodies", "You feel a little stronger. Something in that blood is fighting for you.", "Vous vous sentez un peu plus fort. Quelque chose dans ce sang se bat pour vous.")

# ---------------------------------------------------------------- beaker
for key, en, fr in [
    ("need_water", "Fill the beaker with water first (water bottle or bucket).", "Remplissez d'abord le bécher d'eau (fiole d'eau ou seau)."),
    ("need_reagent", "Use a reagent on the beaker to boil it.", "Utilisez un réactif sur le bécher pour le faire bouillir."),
    ("already_extract", "That's already an extract.", "C'est déjà un extrait."),
    ("need_heat", "The water needs heat below the beaker: campfire, fire, lava or a magma block.", "L'eau doit être chauffée sous le bécher : feu de camp, feu, lave ou bloc de magma."),
    ("started", "%s starts to simmer. About %s seconds.", "%s commence à frémir. Environ %s secondes."),
    ("need_vial", "Use an Empty Vial to collect the extract.", "Utilisez une Fiole vide pour récupérer l'extrait."),
    ("collected", "You collect: %s.", "Vous récupérez : %s."),
    ("done_notify", "A beaker has finished boiling.", "Un bécher a fini de bouillir."),
]:
    t("contagion.beaker." + key, en, fr)

# ---------------------------------------------------------------- journal
for key, en, fr in [
    ("title", "Research Journal", "Journal de recherche"),
    ("author", "the survivors", "les survivants"),
    ("day", "Day %s", "Jour %s"),
    ("pathogen", "Pathogen", "Pathogène"),
    ("type", "Type: %s", "Type : %s"),
    ("needs3", "Needs 3 compounds.", "3 composés requis."),
    ("unidentified", "Unidentified", "Non identifié"),
    ("unidentified_hint", "Analyze infected blood.", "Analysez du sang infecté."),
    ("counts", "Facts: %s | Notes: %s", "Faits : %s | Notes : %s"),
    ("index", "Reagent Index", "Index des réactifs"),
    ("legend.unknown", "unknown", "inconnu"),
    ("legend.inert", "inert (useless)", "inerte (inutile)"),
    ("legend.toxic", "toxic", "toxique"),
    ("legend.active", "holds an active agent", "contient un agent actif"),
    ("category_form", "%s (%s)", "%s (%s)"),
    ("known_facts", "Known Facts", "Faits connus"),
    ("nothing_yet", "Nothing yet.", "Rien pour l'instant."),
    ("notebook", "Lab Notebook", "Carnet de labo"),
    ("newest_first", "newest first", "récents d'abord"),
    ("empty", "Empty.", "Vide."),
    ("manual.tips", "Research tips", "Conseils"),
    ("manual.blood", "Blood tests", "Prises de sang"),
    ("manual.blood.1", "Use a Syringe on someone (sneak + use: yourself), then put the sample on the slide of a Microscope.",
     "Seringue sur quelqu'un (accroupi : sur vous), puis l'échantillon sur la lame d'un Microscope."),
    ("manual.tips.1", "Early infections can test negative. Sicker donors give clearer results.",
     "Une infection récente peut sortir négative. Un donneur plus malade est plus parlant."),
    ("manual.tips.2", "One new fact per donor per day. Veins need hours to recover between draws.",
     "Un fait nouveau par donneur et par jour. Les veines mettent des heures à récupérer."),
    ("manual.cultures", "Cultures", "Cultures"),
    ("manual.cultures.1", "Petri Dish in hand, blood in off-hand: use. It takes time to grow.",
     "Boîte de Petri en main, sang en main secondaire : utiliser. Il faut du temps pour qu'elle pousse."),
    ("manual.cultures.2", "Only blood with enough pathogen grows. Cultures die after a while.",
     "Seul un sang assez chargé pousse. Les cultures finissent par mourir."),
    ("manual.preparing", "Preparing", "Préparation"),
    ("manual.preparing.1", "Mortar in hand, reagent in off-hand: grind it.", "Mortier en main, réactif en main secondaire : broyer."),
    ("manual.preparing.2", "Beaker: water, heat below, then a reagent. Collect the extract with an Empty Vial.",
     "Bécher : de l'eau, de la chaleur dessous, puis un réactif. Récupérez l'extrait avec une Fiole vide."),
    ("manual.mixing", "Mixing serums", "Mélanger"),
    ("manual.mixing.1", "Empty Vial in hand, reagent in off-hand: use. Up to 4. Raw, ground or extract.",
     "Fiole vide en main, réactif en main secondaire : utiliser. Jusqu'à 4. Brut, broyé ou extrait."),
    ("manual.mixing.2", "Sneak + use to seal it.", "Accroupi + utiliser pour sceller."),
    ("manual.testing", "Testing serums", "Tester"),
    ("manual.testing.1", "Microscope: a grown culture on the slide, a sealed serum next to it, then Start.",
     "Microscope : une culture développée sur la lame, un sérum scellé à côté, puis Lancer."),
    ("manual.testing.2", "Safe but slow. Watch for odd reactions: preparation matters.",
     "Sans risque mais lent. Guettez les réactions étranges : la préparation compte."),
    ("manual.treating", "Treating", "Soigner"),
    ("manual.treating.1", "Use a sealed serum twice to inject yourself, or use it on someone.",
     "Utilisez un sérum scellé deux fois pour vous l'injecter, ou sur quelqu'un."),
    ("manual.treating.2", "Risky! Some reagents are toxic.", "Risqué ! Certains réactifs sont toxiques."),
    ("manual.other", "Other tools", "Autres outils"),
    ("manual.other.1", "Thermometer: hold use (on yourself or someone) to check for fever.", "Thermomètre : maintenez utiliser (sur vous ou quelqu'un) pour vérifier la fièvre."),
    ("manual.other.2", "Face Mask: wear it to slow the spread.", "Masque : portez-le pour freiner la contagion."),
    ("manual.other.3", "Sneak + use a Blood Sample on someone to transfuse it.", "Accroupi + échantillon de sang sur quelqu'un : transfusion."),
]:
    t("contagion.book." + key, en, fr)
for key, en, fr in [
    ("chat_header", "=== Research Journal ===", "=== Journal de recherche ==="),
    ("chat_unidentified", "Pathogen: unidentified", "Pathogène : non identifié"),
    ("chat_pathogen", "Pathogen: %s (%s)", "Pathogène : %s (%s)"),
    ("chat_counts", "Facts known: %s, notebook entries: %s", "Faits connus : %s, entrées du carnet : %s"),
    ("chat_open", "Use without sneaking to open the full journal.", "Utilisez sans vous accroupir pour ouvrir le journal complet."),
]:
    t("contagion.journal." + key, en, fr)

# ---------------------------------------------------------------- commands
for key, en, fr in [
    ("released", "The pathogen has been released (%s new host(s)).", "Le pathogène a été libéré (%s nouvel(s) hôte(s))."),
    ("nobody", "There is nobody left to infect.", "Il n'y a plus personne à infecter."),
    ("outbreak", "Somewhere among you, patient zero has been infected. Nobody knows who.", "Quelque part parmi vous, le patient zéro a été infecté. Personne ne sait qui."),
    ("tip", "Tip: craft a Research Journal (book + paper + coal). It has a lab manual and tracks your research.",
     "Astuce : fabriquez un Journal de recherche (livre + papier + charbon). Il contient un manuel et suit vos recherches."),
    ("cured", "Cured %s entities.", "%s entités guéries."),
    ("immunized", "Immunized %s entities.", "%s entités immunisées."),
    ("setday", "Set %s entities to disease day %s.", "%s entités placées au jour %s de la maladie."),
    ("no_strain", "No strain exists yet. It is created at the first infection.", "Aucune souche pour l'instant. Elle est créée à la première infection."),
    ("strain", "Strain: %s (%s)", "Souche : %s (%s)"),
    ("spoiler_cure", "SPOILER cure: %s", "SPOILER remède : %s"),
    ("spoiler_toxic", "Toxic: %s", "Toxiques : %s"),
    ("mutated", "The pathogen mutated into %s. All research is obsolete.", "Le pathogène a muté en %s. Toutes les recherches sont obsolètes."),
]:
    t("contagion.command." + key, en, fr)


# ---------------------------------------------------------------- custom screens (microscope, journal, thermometer HUD)
for key, en, fr in [
    ("slide", "Slide", "Lame"),
    ("serum", "Serum", "Sérum"),
    ("start", "Start", "Lancer"),
    ("examine", "Examine", "Examiner"),
    ("ready", "The results are ready. Press Examine.", "Les résultats sont prêts. Appuyez sur Examiner."),
    ("hint_title", "How to use", "Mode d'emploi"),
    ("hint_blood", "Blood sample on the slide, then Start: analyzes it.", "Échantillon de sang sur la lame, puis Démarrer : l'analyse."),
    ("hint_culture", "A grown culture on the slide plus a sealed serum: tests the serum on it.", "Une culture développée sur la lame et un sérum scellé : teste le sérum dessus."),
    ("empty", "Put a blood sample or a culture on the slide first.", "Placez d'abord un échantillon de sang ou une culture sur la lame."),
    ("need_serum", "To test a culture, put a sealed serum in the serum slot.", "Pour tester une culture, placez un sérum scellé dans l'emplacement du sérum."),
    ("running_analysis", "Analyzing %s's blood...", "Analyse du sang de %s..."),
    ("running_culture", "Testing %s on %s's culture...", "Test de %s sur la culture de %s..."),
]:
    t("contagion.microscope.ui." + key, en, fr)

for key, en, fr in [
    ("overview", "Overview", "Résumé"),
    ("manual", "Lab manual", "Manuel de labo"),
    ("reagents", "Reagents", "Réactifs"),
    ("findings", "Findings", "Découvertes"),
    ("notebook", "Lab notebook", "Carnet de labo"),
]:
    t("contagion.journal.tab." + key, en, fr)
t("contagion.journal.tabs_hint", "Use the bookmarks at the top. Arrow keys or the mouse wheel turn the pages.",
  "Utilisez les marque-pages en haut. Les flèches ou la molette tournent les pages.")
t("contagion.journal.reagents_hint", "Hover over a reagent to see what is known about it.", "Survolez un réactif pour voir ce qu'on en sait.")
t("contagion.journal.prepared_as", "must be: %s", "doit être : %s")
t("contagion.thermometer.hud.measuring", "Measuring...", "Mesure...")

# ---------------------------------------------------------------- advancements
for key, en_title, en_desc, fr_title, fr_desc in [
    ("root", "Contagion", "Something is going around. Stay healthy.", "Contagion", "Quelque chose circule. Restez en bonne santé."),
    ("ounce_of_prevention", "Ounce of Prevention", "Make a Face Mask", "Mieux vaut prévenir", "Fabriquer un Masque"),
    ("under_the_lens", "Under the Lens", "Build a Microscope", "Sous la lentille", "Construire un Microscope"),
    ("first_analysis", "Blood Work", "Analyze a blood sample", "Prise de sang", "Analyser un échantillon de sang"),
    ("identified", "Know Your Enemy", "Identify the pathogen", "Connais ton ennemi", "Identifier le pathogène"),
    ("petri", "Something Is Growing", "Grow a culture in a Petri Dish", "Ça pousse", "Faire pousser une culture dans une Boîte de Petri"),
    ("alchemist", "Kitchen Chemistry", "Boil a reagent into an extract", "Chimie de cuisine", "Faire bouillir un réactif en extrait"),
    ("mad_scientist", "Mad Scientist", "Inject an experimental serum", "Savant fou", "Injecter un sérum expérimental"),
    ("cured", "Survivor", "Be cured of the disease", "Survivant", "Être guéri de la maladie"),
    ("doctor", "Doctor", "Cure someone else", "Docteur", "Guérir quelqu'un d'autre"),
]:
    t(f"advancements.contagion.{key}.title", en_title, fr_title)
    t(f"advancements.contagion.{key}.description", en_desc, fr_desc)

# French typography: non-breaking spaces before : ; ! ? and inside « » (also keeps book lines from breaking badly).
NBSP = "\u00a0"
for k, (en, fr) in list(T.items()):
    for a, b in ((" :", NBSP + ":"), (" ;", NBSP + ";"), (" !", NBSP + "!"), (" ?", NBSP + "?"), ("« ", "«" + NBSP), (" »", NBSP + "»")):
        fr = fr.replace(a, b)
    T[k] = (en, fr)

for i, lang in ((0, "en_us"), (1, "fr_fr")):
    with open(os.path.join(out, lang + ".json"), "w", encoding="utf-8") as f:
        json.dump({k: v[i] for k, v in T.items()}, f, ensure_ascii=False, indent=2)
        f.write("\n")
print(len(T), "keys")
