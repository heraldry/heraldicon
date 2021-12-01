(ns heraldry.coat-of-arms.attributes
  (:require
   [goog.string :as gstring]
   [heraldry.colour :as colour]
   [heraldry.gettext :refer [string]]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def attitude-choices
  [[strings/none :none]
   [(string "Beasts")
    [(string "Cadent") :cadent]
    [(string "Couchant") :couchant]
    [(string "Courant") :courant]
    [(string "Dormant") :dormant]
    [(string "Pascuant") :pascuant]
    [(string "Passant") :passant]
    [(string "Rampant") :rampant]
    [(string "Salient") :salient]
    [(string "Segreant") :segreant]
    [(string "Sejant") :sejant]
    [(string "Sejant erect") :sejant-erect]
    [(string "Statant") :statant]]
   [(string"Two beasts")
    [(string "Addorsed") :addorsed]
    [(string "Combatant") :combatant]
    [(string "Respectant") :respectant]]
   [(string "Winged creatures")
    [(string "Close") :close]
    [(string "Displayed") :displayed]
    [(string "In her piety") :in-her-piety]
    [(string "Issuant") :issuant]
    [(string "Overt") :overt]
    [(string "Recursant") :recursant]
    [(string "Rising") :rising]
    [(string "Vigilant") :vigilant]
    [(string "Volant") :volant]
    [(string "Vulning herself") :vulning]]
   [(string "Sea creatures")
    [(string "Hauriant") :hauriant]
    [(string "Naiant") :naiant]
    [(string "Urinant") :urinant]]
   [(string "Serpents")
    [(string "Glissant") :glissant]
    [(string "Nowed") :nowed]]])

(def attitude-map
  (util/choices->map attitude-choices))

(def facing-choices
  [[strings/none :none]
   [(string "To dexter (default)") :to-dexter]
   [(string "To sinister") :to-sinister]
   [(string "Affronté") :affronte]
   [(string "En arrière") :en-arriere]
   [(string "Guardant") :guardant]
   [(string "In trian aspect") :in-trian-aspect]
   [(string "Reguardant") :reguardant]
   [(string "Salient") :salient]])

(def facing-map
  (util/choices->map facing-choices))

(def attribute-choices
  [[strings/general
    [(string "Erased") :erased]
    [(string "Pierced") :pierced]
    [(string "Voided") :voided]]
   [(string "Ornaments")
    [strings/mantling :mantling]
    [strings/compartment :compartment]
    [strings/supporter :supporter]]
   [(string "Tail")
    [(string "Coward") :coward]
    [(string "Defamed") :defamed]
    [(string "Double queued") :double-queued]
    [(string "Tail nowed") :tail-nowed]
    [(string "Queue fourché saltire reverse") :queue-fourche-saltire-reverse]
    [(string "Queue fourché saltire") :queue-fourche-saltire]
    [(string "Queue fourché") :queue-fourche]]])

(def attribute-map
  (util/choices->map attribute-choices))

(def tincture-modifier-choices
  [[(string "Fauna")
    [(string "Armed") :armed]
    [(string "Attired") :attired]
    [(string "Beaked") :beaked]
    [(string "Combed") :combed]
    [(string "Eyed") :eyed]
    [(string "Eyed (peacock)") :eyed-peacock]
    [(string "Finned") :finned]
    [(string "Incensed") :incensed]
    [(string "Langued") :langued]
    [(string "Legged") :legged]
    [(string "Maned") :maned]
    [(string "Orbed") :orbed]
    [(string "Tailed") :tailed]
    [(string "Toothed") :toothed]
    [(string "Unguled") :unguled]
    [(string "Wattled") :wattled]
    [(string "Winged") :winged]]
   [(string "Flora")
    [(string "Barbed") :barbed]
    [(string "Bladed") :bladed]
    [(string "Fructed") :fructed]
    [(string "Leaved") :leaved]
    [(string "Seeded") :seeded]
    [(string "Slipped") :slipped]]
   [(string "Helmet")
    [(string "Barred") :barred]
    [(string "Trimmed") :trimmed]]
   [(string "Other")
    [(string "Caparisoned") :caparisoned]
    [(string "Collared") :collared]
    [(string "Erased") :erased]
    [(string "Garnished") :garnished]
    [(string "Handled") :handled]
    [(string "Hilted") :hilted]
    [(string "Illuminated") :illuminated]
    [(string "Hooded") :hooded]
    [(string "Masoned") :masoned]
    [(string "Pommeled") :pommeled]
    [(string "Stringed") :stringed]]])

(def tincture-modifier-map
  (util/choices->map tincture-modifier-choices))

(def tincture-modifier-for-charge-choices
  (vec (concat [[(string "Technical")
                 [(string "Don't replace") :keep]
                 [(string "Primary") :primary]
                 [(string "Secondary") :secondary]
                 [(string "Tertiary") :tertiary]
                 [strings/outline :outline]
                 [(string "Shadow (alpha)") :shadow]
                 [(string "Highlight (alpha)") :highlight]
                 [(string "Layer separator") :layer-separator]]]
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
               [[(string "None") :none]]
               (->> qualifier-percentages
                    (map (fn [percentage]
                           (let [{:keys [name key]} (make-qualifier :shadow percentage)]
                             [name key])))))))

(def tincture-modifier-qualifier-for-charge-map
  (util/choices->map tincture-modifier-qualifier-choices))
