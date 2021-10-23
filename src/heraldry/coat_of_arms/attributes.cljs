(ns heraldry.coat-of-arms.attributes
  (:require [heraldry.colour :as colour]
            [heraldry.strings :as strings]
            [heraldry.util :as util]))

(def attitude-choices
  [[strings/none :none]
   [{:en "Beasts"
     :de "Tiere"}
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
   [{:en "Two beasts"
     :de "Zwei Tiere"}
    ["Addorsed" :addorsed]
    ["Combatant" :combatant]
    ["Respectant" :respectant]]
   [{:en "Winged creatures"
     :de "Geflügelte Kreaturen"}
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
   [{:en "Sea creatures"
     :de "Wasser-Kreaturen"}
    ["Hauriant" :hauriant]
    ["Naiant" :naiant]
    ["Urinant" :urinant]]
   [{:en "Serpents"
     :de "Schlangen"}
    ["Glissant" :glissant]
    ["Nowed" :nowed]]])

(def attitude-map
  (util/choices->map attitude-choices))

(def facing-choices
  [[strings/none :none]
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
  [[strings/general
    ["Erased" :erased]
    ["Pierced" :pierced]
    ["Voided" :voided]]
   [{:en "Ornaments"
     :de "Prachtstücke"}
    [strings/mantling :mantling]
    [strings/compartment :compartment]
    [strings/supporter :supporter]]
   [{:en "Tail"
     :de "Schwanz"}
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
   [{:en "Helmet"
     :de "Helm"}
    ["Barred" :barred]
    ["Trimmed" :trimmed]]
   [{:en "Other"
     :de "Andere"}
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
  (concat [[{:en "Technical"
             :de "Technisch"}
            [{:en "Don't replace"
              :de "Nicht ersetzen"} :keep]
            [{:en "Primary"
              :de "Primär"} :primary]
            [{:en "Secondary"
              :de "Sekundär"} :secondary]
            [{:en "Tertiary"
              :de "Tertiär"} :tertiary]
            [strings/outline :outline]
            [strings/shadow :shadow]
            [strings/highlight :highlight]
            [{:en "Layer separator"
              :de "Ebenentrenner"} :layer-separator]]]
          tincture-modifier-choices))

(def tincture-modifier-for-charge-map
  (util/choices->map tincture-modifier-for-charge-choices))

(def tincture-modifier-qualifier-choices
  [["Default" :none]
   ["Shadow"
    ["10%" :shadow-10]
    ["20%" :shadow-20]
    ["30%" :shadow-30]
    ["40%" :shadow-40]
    ["50%" :shadow-50]
    ["60%" :shadow-60]
    ["70%" :shadow-70]
    ["80%" :shadow-80]
    ["90%" :shadow-90]]
   ["Highlight"
    ["10%" :highlight-10]
    ["20%" :highlight-20]
    ["30%" :highlight-30]
    ["40%" :highlight-40]
    ["50%" :highlight-50]
    ["60%" :highlight-60]
    ["70%" :highlight-70]
    ["80%" :highlight-80]
    ["90%" :highlight-90]]])

(def tincture-modifier-qualifier-for-charge-map
  (util/choices->map tincture-modifier-qualifier-choices))

(defn tincture-modifier [value]
  (if (vector? value)
    (first value)
    value))

(defn tincture-modifier-qualifier [value]
  (if (vector? value)
    (second value)
    :none))

(defn opacity-to-grey [opacity]
  (let [v (-> opacity (* 255) int)]
    (colour/hex-colour v v v)))

(def shadow-qualifiers
  {:shadow-10 (opacity-to-grey 0.1)
   :shadow-20 (opacity-to-grey 0.2)
   :shadow-30 (opacity-to-grey 0.3)
   :shadow-40 (opacity-to-grey 0.4)
   :shadow-50 (opacity-to-grey 0.5)
   :shadow-60 (opacity-to-grey 0.6)
   :shadow-70 (opacity-to-grey 0.7)
   :shadow-80 (opacity-to-grey 0.8)
   :shadow-90 (opacity-to-grey 0.9)})

(def highlight-qualifiers
  {:highlight-10 (opacity-to-grey 0.1)
   :highlight-20 (opacity-to-grey 0.2)
   :highlight-30 (opacity-to-grey 0.3)
   :highlight-40 (opacity-to-grey 0.4)
   :highlight-50 (opacity-to-grey 0.5)
   :highlight-60 (opacity-to-grey 0.6)
   :highlight-70 (opacity-to-grey 0.7)
   :highlight-80 (opacity-to-grey 0.8)
   :highlight-90 (opacity-to-grey 0.9)})
