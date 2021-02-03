(ns heraldry.coat-of-arms.attributes
  (:require [heraldry.util :as util]))

(def attitude-options
  [["None" :none]
   ["Beasts"
    ["Couchant" :couchant]
    ["Courant" :courant]
    ["Dormant" :dormant]
    ["Pascuant" :pascuant]
    ["Passant" :passant]
    ["Rampant" :rampant]
    ["Salient" :salient]
    ["Segreant" :segreant]
    ["Sejant" :sejant]
    ["Statant" :statant]]
   ["Two beasts"
    ["Addorsed" :addorsed]
    ["Combatant" :combatant]
    ["Respectant" :respectant]]
   ["Winged creatures"
    ["Close" :close]
    ["Displayed" :displayed]
    ["In her piety" :in-her-piety]
    ["Issuant" :issuant]
    ["Overt" :overt]
    ["Recursant" :recursant]
    ["Rising" :rising]
    ["Vigilant" :vigilant]
    ["Volant" :volant]
    ["Vulning herself" :vulning]]
   ["Sea creatures"
    ["Hauriant" :hauriant]
    ["Naiant" :naiant]
    ["Urinant" :urinant]]
   ["Serpents"
    ["Glissant" :glissant]
    ["Nowed" :nowed]]])

(def attitude-map
  (util/options->map attitude-options))

(def facing-options
  [["None" :none]
   ["To dexter (default)" :to-dexter]
   ["To sinister" :to-sinister]
   ["Affronté" :affronte]
   ["En arrière" :en-arriere]
   ["Guardant" :guardant]
   ["In trian aspect" :in-trian-aspect]
   ["Reguardant" :reguardant]
   ["Salient" :salient]])

(def facing-map
  (util/options->map facing-options))

(def attribute-options
  [["General"
    ["Erased" :erased]
    ["Pierced" :pierced]
    ["Voided" :voided]]
   ["Tail"
    ["Coward" :coward]
    ["Defamed" :defamed]
    ["Double queued" :double-queued]
    ["Nowed" :tail-nowed]
    ["Queue fourchée saltire reversed" :queue-fourchee-saltire-reversed]
    ["Queue fourchée saltire" :queue-fourchee-saltire]
    ["Queue fourchée" :queue-fourchee]]])

(def attribute-map
  (util/options->map attribute-options))

(def tincture-modifier-options
  [["Fauna"
    ["Armed" :armed]
    ["Attired" :attired]
    ["Beaked" :beaked]
    ["Eyed (peacock)" :eyed]
    ["Jelloped" :jelloped]
    ["Langued" :langued]
    ["Legged" :legged]
    ["Unguled" :unguled]
    ["Winged" :winged]]
   ["Flora"
    ["Barbed" :barbed]
    ["Bladed" :bladed]
    ["Fructed" :fructed]
    ["Leaved" :leaved]
    ["Seeded" :seeded]
    ["Slipped" :slipped]]
   ["Other"
    ["Caparisoned" :caparisoned]
    ["Collared" :collared]
    ["Erased" :erased]
    ["Garnished" :garnished]
    ["Hilt" :hilt]
    ["Hooded" :hooded]
    ["Masoned" :masoned]
    ["Pommel" :pommel]
    ["Stringed" :stringed]]])

(def tincture-modifier-map
  (util/options->map tincture-modifier-options))

(def tincture-modifier-for-charge-options
  (concat [["Technical"
            ["Don't replace" :keep]
            ["Primary" :primary]
            ["Outline" :outline]
            ["Eyes/teeth" :eyes-and-teeth]]]
          tincture-modifier-options))

(def tincture-modifier-for-charge-map
  (util/options->map tincture-modifier-for-charge-options))
