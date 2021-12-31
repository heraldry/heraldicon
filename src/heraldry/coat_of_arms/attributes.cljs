(ns heraldry.coat-of-arms.attributes
  (:require
   [goog.string :as gstring]
   [heraldry.colour :as colour]
   [heraldry.util :as util]))

(def attitude-choices
  [[:string.charge.attitude/none :none]
   [:string.charge.attitude.group/beasts
    [:string.charge.attitude/cadent :cadent]
    [:string.charge.attitude/couchant :couchant]
    [:string.charge.attitude/courant :courant]
    [:string.charge.attitude/dormant :dormant]
    [:string.charge.attitude/pascuant :pascuant]
    [:string.charge.attitude/passant :passant]
    [:string.charge.attitude/rampant :rampant]
    [:string.charge.attitude/salient :salient]
    [:string.charge.attitude/segreant :segreant]
    [:string.charge.attitude/sejant :sejant]
    [:string.charge.attitude/sejant-erect :sejant-erect]
    [:string.charge.attitude/statant :statant]]
   [:string.charge.attitude.group/two-beasts
    [:string.charge.attitude/addorsed :addorsed]
    [:string.charge.attitude/combatant :combatant]
    [:string.charge.attitude/respectant :respectant]]
   [:string.charge.attitude.group/winged-creatures
    [:string.charge.attitude/close :close]
    [:string.charge.attitude/displayed :displayed]
    [:string.charge.attitude/in-her-piety :in-her-piety]
    [:string.charge.attitude/issuant :issuant]
    [:string.charge.attitude/overt :overt]
    [:string.charge.attitude/recursant :recursant]
    [:string.charge.attitude/rising :rising]
    [:string.charge.attitude/vigilant :vigilant]
    [:string.charge.attitude/volant :volant]
    [:string.charge.attitude/vulning :vulning]]
   [:string.charge.attitude.group/sea-creatures
    [:string.charge.attitude/hauriant :hauriant]
    [:string.charge.attitude/naiant :naiant]
    [:string.charge.attitude/urinant :urinant]]
   [:string.charge.attitude.group/serpents
    [:string.charge.attitude/glissant :glissant]
    [:string.charge.attitude/nowed :nowed]]])

(def attitude-map
  (util/choices->map attitude-choices))

(def facing-choices
  [[:string.charge.facing/none :none]
   [:string.charge.facing/to-dexter :to-dexter]
   [:string.charge.facing/to-sinister :to-sinister]
   [:string.charge.facing/affronte :affronte]
   [:string.charge.facing/en-arriere :en-arriere]
   [:string.charge.facing/guardant :guardant]
   [:string.charge.facing/in-trian-aspect :in-trian-aspect]
   [:string.charge.facing/reguardant :reguardant]
   [:string.charge.facing/salient :salient]])

(def facing-map
  (util/choices->map facing-choices))

(def attribute-choices
  [[:string.charge.attribute.group/general
    [:string.charge.attribute/erased :erased]
    [:string.charge.attribute/pierced :pierced]
    [:string.charge.attribute/voided :voided]]
   [:string.charge.attribute.group/ornaments
    [:string.charge.attribute/mantling :mantling]
    [:string.charge.attribute/compartment :compartment]
    [:string.charge.attribute/supporter :supporter]]
   [:string.charge.attribute.group/tail
    [:string.charge.attribute/coward :coward]
    [:string.charge.attribute/defamed :defamed]
    [:string.charge.attribute/double-queued :double-queued]
    [:string.charge.attribute/tail-nowed :tail-nowed]
    [:string.charge.attribute/queue-fourche-saltire-reverse :queue-fourche-saltire-reverse]
    [:string.charge.attribute/queue-fourche-saltire :queue-fourche-saltire]
    [:string.charge.attribute/queue-fourche :queue-fourche]]])

(def attribute-map
  (util/choices->map attribute-choices))

(def tincture-modifier-choices
  [[:string.charge.tincture-modifier.group/fauna
    [:string.charge.tincture-modifier/armed :armed]
    [:string.charge.tincture-modifier/attired :attired]
    [:string.charge.tincture-modifier/beaked :beaked]
    [:string.charge.tincture-modifier/combed :combed]
    [:string.charge.tincture-modifier/eyed :eyed]
    [:string.charge.tincture-modifier/eyed-peacock :eyed-peacock]
    [:string.charge.tincture-modifier/finned :finned]
    [:string.charge.tincture-modifier/incensed :incensed]
    [:string.charge.tincture-modifier/langued :langued]
    [:string.charge.tincture-modifier/legged :legged]
    [:string.charge.tincture-modifier/maned :maned]
    [:string.charge.tincture-modifier/orbed :orbed]
    [:string.charge.tincture-modifier/tailed :tailed]
    [:string.charge.tincture-modifier/toothed :toothed]
    [:string.charge.tincture-modifier/unguled :unguled]
    [:string.charge.tincture-modifier/wattled :wattled]
    [:string.charge.tincture-modifier/winged :winged]]
   [:string.charge.tincture-modifier.group/flora
    [:string.charge.tincture-modifier/barbed :barbed]
    [:string.charge.tincture-modifier/bladed :bladed]
    [:string.charge.tincture-modifier/fructed :fructed]
    [:string.charge.tincture-modifier/leaved :leaved]
    [:string.charge.tincture-modifier/seeded :seeded]
    [:string.charge.tincture-modifier/slipped :slipped]]
   [:string.charge.tincture-modifier.group/helmet
    [:string.charge.tincture-modifier/barred :barred]
    [:string.charge.tincture-modifier/trimmed :trimmed]]
   [:string.charge.tincture-modifier.group/other
    [:string.charge.tincture-modifier/caparisoned :caparisoned]
    [:string.charge.tincture-modifier/collared :collared]
    [:string.charge.tincture-modifier/erased :erased]
    [:string.charge.tincture-modifier/garnished :garnished]
    [:string.charge.tincture-modifier/handled :handled]
    [:string.charge.tincture-modifier/hilted :hilted]
    [:string.charge.tincture-modifier/illuminated :illuminated]
    [:string.charge.tincture-modifier/hooded :hooded]
    [:string.charge.tincture-modifier/masoned :masoned]
    [:string.charge.tincture-modifier/pommeled :pommeled]
    [:string.charge.tincture-modifier/stringed :stringed]]])

(def tincture-modifier-map
  (util/choices->map tincture-modifier-choices))

(def tincture-modifier-for-charge-choices
  (vec (concat [[:string.charge.tincture-modifier.group/technical
                 [:string.charge.tincture-modifier.special/keep :keep]
                 [:string.charge.tincture-modifier.special/primary :primary]
                 [:string.charge.tincture-modifier.special/secondary :secondary]
                 [:string.charge.tincture-modifier.special/tertiary :tertiary]
                 [:string.charge.tincture-modifier.special/outline :outline]
                 [:string.charge.tincture-modifier.special/shadow :shadow]
                 [:string.charge.tincture-modifier.special/highlight :highlight]
                 [:string.charge.tincture-modifier.special/layer-separator :layer-separator]]]
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
                                             :string.option/shadow
                                             :string.option/highlight))
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
               [[:string.charge.tincture-modifier/none :none]]
               (->> qualifier-percentages
                    (map (fn [percentage]
                           (let [{:keys [name key]} (make-qualifier :shadow percentage)]
                             [name key])))))))

(def tincture-modifier-qualifier-for-charge-map
  (util/choices->map tincture-modifier-qualifier-choices))
