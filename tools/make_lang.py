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
    ("petri_dish", ("A shallow glass dish with a lid", "Une boîte en verre peu profonde"), ("Use with something in your other hand", "S'utilise avec quelque chose dans l'autre main")),
    ("mortar", ("For crushing things", "Pour écraser des choses"), ("Use with something in your other hand", "S'utilise avec quelque chose dans l'autre main")),
    ("empty_vial", ("A small corked vial", "Une petite fiole bouchée"), ("Use with something in your other hand", "S'utilise avec quelque chose dans l'autre main")),
    ("thermometer", ("Hold use to take your temperature", "Maintenir utiliser pour prendre votre température"), ("Hold use on someone to take theirs", "Maintenir sur quelqu'un pour la sienne")),
    ("research_journal", ("Everyone's shared notes", "Les notes communes"), ("Use to read", "Utiliser pour lire")),
    ("face_mask", ("Wear on your head", "Se porte sur la tête"), ("Keeps your breath to yourself", "Garde votre souffle pour vous")),
    ("microscope", ("For a closer look", "Pour y regarder de plus près"), ("Takes its time", "Prend son temps")),
    ("beaker", ("Heat-proof glassware", "Verrerie résistante à la chaleur"), ("Needs water, and heat below", "Il lui faut de l'eau, et du feu dessous")),
]:
    t(f"contagion.lore.{name}.1", *l1)
    t(f"contagion.lore.{name}.2", *l2)

t("contagion.lore.sample.donor", "Donor: %s", "Donneur : %s")
t("contagion.lore.sample.day", "Drawn on day %s", "Prélevé le jour %s")
t("contagion.lore.culture.donor", "Blood of %s (day %s)", "Sang de %s (jour %s)")
t("contagion.lore.culture.growing", "Growing...", "En croissance...")
t("contagion.lore.culture.colonized", "Colonies have grown", "Des colonies se sont développées")
t("contagion.lore.culture.sterile", "Nothing grew", "Rien n'a poussé")
t("contagion.lore.culture.dead", "The culture has died", "La culture est morte")
t("contagion.lore.serum.sealed", "Sealed experimental serum", "Sérum expérimental scellé")
t("contagion.lore.serum.unsealed", "Unsealed mixture (%s/%s)", "Mélange non scellé (%s/%s)")
t("contagion.lore.serum.sealed_use1", "Use twice to inject yourself, or use on someone", "Utiliser deux fois pour vous l'injecter, ou sur quelqu'un")
t("contagion.lore.serum.unsealed_use1", "Use with something in your other hand to add it", "Utiliser avec quelque chose dans l'autre main pour l'ajouter")
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
    ("dish_need_sample", "You need something in your other hand to put in the dish.", "Il vous faut quelque chose dans l'autre main à mettre dans la boîte."),
    ("inoculated", "You smear %s's blood on the dish and close the lid.", "Vous étalez le sang de %s dans la boîte et refermez le couvercle."),
    ("culture_ready", "Something has grown in the dish.", "Quelque chose a poussé dans la boîte."),
    ("culture_sterile", "Nothing grew in that dish.", "Rien n'a poussé dans cette boîte."),
    ("culture_dead", "This culture has died.", "Cette culture est morte."),
    ("mortar_need_reagent", "Nothing worth grinding in your other hand.", "Rien qui vaille la peine d'être broyé dans votre autre main."),
    ("mortar_only_raw", "That has already been prepared.", "C'est déjà préparé."),
    ("ground", "You grind it into %s.", "Vous le broyez : %s."),
    ("vial_need_reagent", "Nothing worth putting in the vial in your other hand.", "Rien qui vaille la peine d'aller dans la fiole dans votre autre main."),
    ("mixture_started", "Added %s to a new mixture.", "%s ajouté à un nouveau mélange."),
    ("mixture_need_reagent", "Nothing worth adding in your other hand. Sneak + use to seal the vial.", "Rien qui vaille la peine d'être ajouté dans votre autre main. Accroupi + utiliser pour sceller la fiole."),
    ("mixture_full", "The vial is full. Sneak + use to seal it.", "La fiole est pleine. Accroupi + utiliser pour la sceller."),
    ("mixture_duplicate", "This mixture already contains %s.", "Ce mélange contient déjà %s."),
    ("mixture_added", "Added %s (%s/%s).", "%s ajouté (%s/%s)."),
    ("sealed", "Serum sealed.", "Sérum scellé."),
]:
    t("contagion.hint." + key, en, fr)

# ---------------------------------------------------------------- microscope & culture tests
for key, en, fr in [
    ("start_analysis", "You prepare a slide of %s's blood.", "Vous préparez une lame du sang de %s."),
    ("start_culture", "You expose %s's culture to the serum.", "Vous exposez la culture de %s au sérum."),
    ("seal_first", "The serum isn't sealed.", "Le sérum n'est pas scellé."),
    ("culture_growing", "Nothing to see yet in that dish.", "Rien à voir dans cette boîte pour l'instant."),
    ("culture_sterile", "Nothing grew in that dish. There's nothing to test on.", "Rien n'a poussé dans cette boîte. Rien à tester."),
    ("culture_dead", "That culture is dead.", "Cette culture est morte."),
    ("done_notify", "The microscope has finished.", "Le microscope a terminé."),
    ("analysis_header", "Blood analysis: %s (drawn day %s)", "Analyse de sang : %s (prélevé le jour %s)"),
    ("culture_header", "Culture test: %s", "Test sur culture : %s"),
    ("degraded", "The sample has degraded too much to read.", "L'échantillon est trop dégradé pour être lu."),
    ("antibodies", "There's something in this blood that attacks the pathogen.", "Il y a dans ce sang quelque chose qui attaque le pathogène."),
    ("negative", "No pathogen detected.", "Aucun pathogène détecté."),
    ("positive", "Pathogen present. Viral load: %s.", "Pathogène présent. Charge virale : %s."),
    ("identified", "Identified as %s, a %s pathogen.", "Identifié : %s, un pathogène %s."),
    ("nothing_new_donor", "It looks just like the last slide of %s's blood.", "Ça ressemble exactement à la dernière lame du sang de %s."),
    ("insight", "You notice something: %s", "Vous remarquez quelque chose : %s"),
    ("no_insight", "Nothing new on this slide.", "Rien de nouveau sur cette lame."),
    ("recorded", "(Noted in the Research Journal.)", "(Noté dans le Journal de recherche.)"),
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
t("contagion.fact.category", "The pathogen's membrane seemed to weaken around traces of %s matter.",
  "La membrane du pathogène a semblé s'affaiblir au contact de traces de matière de type %s.")
t("contagion.fact.form", "Whatever it is in the %s matter, it reacted more when %s.", "Ce qu'il y a dans la matière de type %s réagissait davantage une fois %s.")
t("contagion.fact.toxic", "%s destroyed healthy cells in the sample.", "%s a détruit des cellules saines de l'échantillon.")
t("contagion.fact.inert", "%s did nothing at all to the pathogen.", "%s n'a eu absolument aucun effet sur le pathogène.")

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
    ("need_water", "It's empty.", "Il est vide."),
    ("need_reagent", "Nothing worth boiling in your hand.", "Rien qui vaille la peine d'être bouilli dans votre main."),
    ("already_extract", "That's already an extract.", "C'est déjà un extrait."),
    ("need_heat", "The water stays cold.", "L'eau reste froide."),
    ("started", "%s starts to simmer.", "%s commence à frémir."),
    ("need_vial", "You'll need something to pour it into.", "Il vous faut quelque chose pour le verser."),
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
    ("unidentified", "Unknown", "Inconnu"),
    ("counts", "Facts: %s | Notes: %s", "Faits : %s | Notes : %s"),
    ("index", "Tried substances", "Substances essayées"),
    ("legend.inert", "did nothing", "sans effet"),
    ("legend.toxic", "damaged healthy cells", "a abîmé des cellules saines"),
    ("known_facts", "Observations", "Observations"),
    ("nothing_yet", "Nothing yet.", "Rien pour l'instant."),
    ("notebook", "Lab Notebook", "Carnet de labo"),
    ("nothing_tried", "Nothing yet.", "Rien pour l'instant."),
    ("tried", "Tried: %s", "Essayé : %s"),
    ("gear", "Equipment", "Matériel"),
    ("gear.syringe", "Syringe: draws blood. Crouch to use it on yourself.", "Seringue : prélève du sang. Accroupissez-vous pour l'utiliser sur vous."),
    ("gear.other_hand", "Dish, mortar and vials: they work on whatever is in your other hand.", "Boîte, mortier et fioles : ils agissent sur ce que vous tenez dans l'autre main."),
    ("gear.beaker", "Beaker: water inside, fire beneath, and patience.", "Bécher : de l'eau dedans, du feu dessous, et de la patience."),
    ("gear.microscope", "Microscope: look through it. It is slow.", "Microscope : regardez dedans. Il est lent."),
    ("gear.vial", "A vial can be sealed (crouch + use). What's sealed can be injected.", "Une fiole se scelle (accroupi + utiliser). Ce qui est scellé peut s'injecter."),
    ("gear.thermometer", "Thermometer: hold it a while.", "Thermomètre : tenez-le un moment."),
    ("gear.mask", "Mask: worn on the face.", "Masque : se porte sur le visage."),
    ("gear.blood", "Blood can go back into a vein, too (crouch).", "Le sang peut aussi retourner dans une veine (accroupi)."),
    ("newest_first", "newest first", "récents d'abord"),
    ("empty", "There's nothing on the slide.", "Il n'y a rien sur la lame."),
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
    ("tip", "Someone should keep notes. (Research Journal: book + paper + coal.)", "Quelqu'un devrait prendre des notes. (Journal de recherche : livre + papier + charbon.)"),
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
    ("ready", "Done.", "Terminé."),
    ("hint", "Nothing on the slide.", "Rien sur la lame."),
    ("empty", "Put a blood sample or a culture on the slide first.", "Placez d'abord un échantillon de sang ou une culture sur la lame."),
    ("need_serum", "Just a culture. Nothing new to see.", "Juste une culture. Rien de nouveau à voir."),
    ("running_analysis", "Analyzing %s's blood...", "Analyse du sang de %s..."),
    ("running_culture", "Testing %s on %s's culture...", "Test de %s sur la culture de %s..."),
]:
    t("contagion.microscope.ui." + key, en, fr)

for key, en, fr in [
    ("overview", "Overview", "Résumé"),
    ("equipment", "Equipment", "Matériel"),
    ("substances", "Substances", "Substances"),
    ("findings", "Observations", "Observations"),
    ("notebook", "Lab notebook", "Carnet de labo"),
]:
    t("contagion.journal.tab." + key, en, fr)
t("contagion.thermometer.hud.measuring", "Measuring...", "Mesure...")

# ---------------------------------------------------------------- advancements
for key, en_title, en_desc, fr_title, fr_desc in [
    ("root", "Contagion", "Something is going around. Stay healthy.", "Contagion", "Quelque chose circule. Restez en bonne santé."),
    ("ounce_of_prevention", "Ounce of Prevention", "Make a Face Mask", "Mieux vaut prévenir", "Fabriquer un Masque"),
    ("under_the_lens", "Under the Lens", "Build a Microscope", "Sous la lentille", "Construire un Microscope"),
    ("first_analysis", "Blood Work", "Take a closer look at someone's blood", "Prise de sang", "Regarder de plus près le sang de quelqu'un"),
    ("identified", "Know Your Enemy", "Put a name on it", "Connais ton ennemi", "Mettre un nom dessus"),
    ("petri", "Something Is Growing", "Try something on what you grew", "Ça pousse", "Essayer quelque chose sur ce que vous avez fait pousser"),
    ("alchemist", "Kitchen Chemistry", "Cook something that isn't food", "Chimie de cuisine", "Cuisiner quelque chose qui ne se mange pas"),
    ("mad_scientist", "Mad Scientist", "Try it on a human", "Savant fou", "Essayer sur un humain"),
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
