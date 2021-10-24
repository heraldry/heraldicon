(ns heraldry.coat-of-arms.attributes
  (:require [goog.string :as gstring]
            [heraldry.colour :as colour]
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
  (vec (concat [[{:en "Technical"
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
                 [{:en "Shadow (alpha)"
                   :de "Schatten (alpha)"} :shadow]
                 [{:en "Highlight (alpha)"
                   :de "Glanz (alpha)"} :highlight]
                 [{:en "Layer separator"
                   :de "Ebenentrenner"} :layer-separator]]]
               tincture-modifier-choices)))

(def tincture-modifier-for-charge-map
  (util/choices->map tincture-modifier-for-charge-choices))

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

(defn make-qualifier [kind percentage]
  (let [qualifier-name (str percentage "%")
        keyword-suffix (gstring/format "%02d" percentage)
        colour (opacity-to-grey (/ percentage 100))]
    {:kind :shadow
     :key (keyword (str (name kind) "-" keyword-suffix))
     :name (util/str-tr qualifier-name " " (if (= kind :shadow)
                                             strings/shadow
                                             strings/highlight))
     :colour colour}))

(def qualifier-percentages
  (range 5 100 5))

(def shadow-qualifiers
  (->> qualifier-percentages
       (map (fn [percentage]
              (let [{:keys [key colour]} (make-qualifier :shadow percentage)]
                [key colour])))
       (into {})))

(def highlight-qualifiers
  (->> qualifier-percentages
       (map (fn [percentage]
              (let [{:keys [key colour]} (make-qualifier :highlight percentage)]
                [key colour])))
       (into {})))


(def tincture-modifier-qualifier-choices
  (vec (concat (->> qualifier-percentages
                    reverse
                    (map (fn [percentage]
                           (let [{:keys [name key]} (make-qualifier :highlight percentage)]
                             [name key]))))
               [[{:en "None"
                  :de "Keine"} :none]]
               (->> qualifier-percentages
                    (map (fn [percentage]
                           (let [{:keys [name key]} (make-qualifier :shadow percentage)]
                             [name key])))))))

(def tincture-modifier-qualifier-for-charge-map
  (util/choices->map tincture-modifier-qualifier-choices))
