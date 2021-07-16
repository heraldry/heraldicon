(ns heraldry.coat-of-arms.attributes
  (:require [heraldry.util :as util]))

(def attitude-choices
  [["None" :none]
   ["Beasts"
    ["Cadent" :cadent]
    ["Couchant" :couchant]
    ["Courant" :courant]
    ["Dormant" :dormant]
    ["Pascuant" :pascuant]
    ["Passant" :passant]
    ["Rampant" :rampant]
    ["Salient" :salient]
    ["Segreant" :segreant]
    ["Sejant" :sejant]
    ["Sejant erect" :sejant-erect]
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
  (util/choices->map attitude-choices))

(def facing-choices
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
  (util/choices->map facing-choices))

(def attribute-choices
  [["General"
    ["Erased" :erased]
    ["Pierced" :pierced]
    ["Voided" :voided]]
   ["Tail"
    ["Coward" :coward]
    ["Defamed" :defamed]
    ["Double queued" :double-queued]
    ["Tail nowed" :tail-nowed]
    ["Queue fourché saltire reverse" :queue-fourche-saltire-reverse]
    ["Queue fourché saltire" :queue-fourche-saltire]
    ["Queue fourché" :queue-fourche]]])

(def attribute-map
  (util/choices->map attribute-choices))

(def tincture-modifier-choices
  [["Fauna"
    ["Armed" :armed]
    ["Attired" :attired]
    ["Beaked" :beaked]
    ["Combed" :combed]
    ["Eyed" :eyed]
    ["Eyed (peacock)" :eyed-peacock]
    ["Finned" :finned]
    ["Incensed" :incensed]
    ["Langued" :langued]
    ["Legged" :legged]
    ["Maned" :maned]
    ["Orbed" :orbed]
    ["Tailed" :tailed]
    ["Toothed" :toothed]
    ["Unguled" :unguled]
    ["Wattled" :wattled]
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
    ["Handled" :handled]
    ["Hilted" :hilted]
    ["Illuminated" :illuminated]
    ["Hooded" :hooded]
    ["Masoned" :masoned]
    ["Pommeled" :pommeled]
    ["Stringed" :stringed]]])

(def tincture-modifier-map
  (util/choices->map tincture-modifier-choices))

(def tincture-modifier-for-charge-choices
  (concat [["Technical"
            ["Don't replace" :keep]
            ["Primary" :primary]
            ["Secondary" :secondary]
            ["Tertiary" :tertiary]
            ["Outline" :outline]
            ["Shadow" :shadow]
            ["Highlight" :highlight]]]
          tincture-modifier-choices))

(def tincture-modifier-for-charge-map
  (util/choices->map tincture-modifier-for-charge-choices))
