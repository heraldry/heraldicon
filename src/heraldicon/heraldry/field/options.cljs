(ns heraldicon.heraldry.field.options
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.type.barry :as barry]
   [heraldicon.heraldry.field.type.bendy :as bendy]
   [heraldicon.heraldry.field.type.bendy-sinister :as bendy-sinister]
   [heraldicon.heraldry.field.type.chequy :as chequy]
   [heraldicon.heraldry.field.type.chevronny :as chevronny]
   [heraldicon.heraldry.field.type.counterchanged :as counterchanged]
   [heraldicon.heraldry.field.type.fretty :as fretty]
   [heraldicon.heraldry.field.type.gyronny :as gyronny]
   [heraldicon.heraldry.field.type.gyronny-n :as gyronny-n]
   [heraldicon.heraldry.field.type.lozengy :as lozengy]
   [heraldicon.heraldry.field.type.masony :as masony]
   [heraldicon.heraldry.field.type.paly :as paly]
   [heraldicon.heraldry.field.type.papellony :as papellony]
   [heraldicon.heraldry.field.type.per-bend :as per-bend]
   [heraldicon.heraldry.field.type.per-bend-sinister :as per-bend-sinister]
   [heraldicon.heraldry.field.type.per-chevron :as per-chevron]
   [heraldicon.heraldry.field.type.per-fess :as per-fess]
   [heraldicon.heraldry.field.type.per-pale :as per-pale]
   [heraldicon.heraldry.field.type.per-pile :as per-pile]
   [heraldicon.heraldry.field.type.per-saltire :as per-saltire]
   [heraldicon.heraldry.field.type.plain :as plain]
   [heraldicon.heraldry.field.type.potenty :as potenty]
   [heraldicon.heraldry.field.type.quartered :as quartered]
   [heraldicon.heraldry.field.type.quarterly :as quarterly]
   [heraldicon.heraldry.field.type.tierced-per-fess :as tierced-per-fess]
   [heraldicon.heraldry.field.type.tierced-per-pale :as tierced-per-pale]
   [heraldicon.heraldry.field.type.tierced-per-pall :as tierced-per-pall]
   [heraldicon.heraldry.field.type.vairy :as vairy]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(derive :heraldry.field/type :heraldry/field)

(def ^:private fields
  [plain/field-type
   per-fess/field-type
   per-pale/field-type
   per-bend/field-type
   per-bend-sinister/field-type
   per-chevron/field-type
   per-saltire/field-type
   quartered/field-type
   quarterly/field-type
   gyronny/field-type
   gyronny-n/field-type
   tierced-per-fess/field-type
   tierced-per-pale/field-type
   tierced-per-pall/field-type
   per-pile/field-type
   barry/field-type
   paly/field-type
   bendy/field-type
   bendy-sinister/field-type
   chevronny/field-type
   chequy/field-type
   lozengy/field-type
   vairy/field-type
   potenty/field-type
   papellony/field-type
   masony/field-type
   fretty/field-type
   counterchanged/field-type])

(def choices
  (->> fields
       (map (fn [key]
              (derive key :heraldry.field/type)
              [(field.interface/display-name key) key]))
       vec))

(def field-map
  (options/choices->map choices))

(def ^:private type-option
  {:type :option.type/choice
   :choices choices
   :ui/label :string.option/partition
   :ui/element :ui.element/field-type-select})

(derive :heraldry/field :heraldry.options/root)

(defn- fess-group-options [context]
  (let [ordinary-type :heraldry.ordinary.type/fess
        {:keys [affected-paths
                default-spacing
                default-size]} (interface/get-auto-ordinary-info ordinary-type context)
        auto-positioned? (seq affected-paths)]
    (when auto-positioned?
      {:default-size {:type :option.type/range
                      :min 0.1
                      :max 90
                      :default default-size
                      :ui/label :string.option/default-size
                      :ui/step 0.1}
       :default-spacing {:type :option.type/range
                         :min -75
                         :max 75
                         :default default-spacing
                         :ui/label :string.option/default-spacing
                         :ui/step 0.1}
       :offset-y {:type :option.type/range
                  :min -75
                  :max 75
                  :default 0
                  :ui/label :string.option/offset-y
                  :ui/step 0.1}})))

(defn- pale-group-options [context]
  (let [ordinary-type :heraldry.ordinary.type/pale
        {:keys [affected-paths
                default-spacing
                default-size]} (interface/get-auto-ordinary-info ordinary-type context)
        auto-positioned? (seq affected-paths)]
    (when auto-positioned?
      {:default-size {:type :option.type/range
                      :min 0.1
                      :max 90
                      :default default-size
                      :ui/label :string.option/default-size
                      :ui/step 0.1}
       :default-spacing {:type :option.type/range
                         :min -75
                         :max 75
                         :default default-spacing
                         :ui/label :string.option/default-spacing
                         :ui/step 0.1}
       :offset-x {:type :option.type/range
                  :min -75
                  :max 75
                  :default 0
                  :ui/label :string.option/offset-x
                  :ui/step 0.1}})))

(defn- chevron-group-options [context]
  (let [ordinary-type :heraldry.ordinary.type/chevron
        {:keys [affected-paths
                default-spacing
                default-size]} (interface/get-auto-ordinary-info ordinary-type context)
        auto-positioned? (seq affected-paths)
        origin-point-option {:type :option.type/choice
                             :choices (position/orientation-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top-right
                                        :bottom-left
                                        :bottom-right
                                        :angle])
                             :default :base
                             :ui/label :string.option/point}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :chevron-group :origin :point))
                              origin-point-option)]
    (when auto-positioned?
      {:origin (cond-> {:point origin-point-option
                        :ui/label :string.charge.attitude/issuant
                        :ui/element :ui.element/position}

                 (= current-origin-point
                    :angle) (assoc :angle {:type :option.type/range
                                           :min -180
                                           :max 180
                                           :default 0
                                           :ui/label :string.option/angle})

                 (not= current-origin-point
                       :angle) (assoc :offset-x {:type :option.type/range
                                                 :min -50
                                                 :max 50
                                                 :default 0
                                                 :ui/label :string.option/offset-x
                                                 :ui/step 0.1}
                                      :offset-y {:type :option.type/range
                                                 :min -75
                                                 :max 75
                                                 :default 0
                                                 :ui/label :string.option/offset-y
                                                 :ui/step 0.1}))
       :orientation {:point {:type :option.type/choice
                             :choices (position/orientation-choices [:angle])
                             :default :angle
                             :ui/label :string.option/point}
                     :angle {:type :option.type/range
                             :min 0
                             :max 360
                             :default 45
                             :ui/label :string.option/angle}
                     :ui/label :string.option/orientation
                     :ui/element :ui.element/position}
       :default-size {:type :option.type/range
                      :min 0.1
                      :max 90
                      :default default-size
                      :ui/label :string.option/default-size
                      :ui/step 0.1}
       :default-spacing {:type :option.type/range
                         :min -75
                         :max 75
                         :default default-spacing
                         :ui/label :string.option/default-spacing
                         :ui/step 0.1}
       :offset-x {:type :option.type/range
                  :min -75
                  :max 75
                  :default 0
                  :ui/label :string.option/offset-x
                  :ui/step 0.1}
       :offset-y {:type :option.type/range
                  :min -75
                  :max 75
                  :default 0
                  :ui/label :string.option/offset-y
                  :ui/step 0.1}})))

(defn- bend-group-options [context]
  (let [ordinary-type :heraldry.ordinary.type/bend
        {:keys [affected-paths
                default-spacing
                default-size]} (interface/get-auto-ordinary-info ordinary-type context)
        auto-positioned? (seq affected-paths)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            [:fess
                                             :chief
                                             :base
                                             :honour
                                             :nombril
                                             :bottom-right
                                             :hoist
                                             :fly
                                             :center
                                             :angle])
                                  :default :fess
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :bend-group :orientation :point))
                                   orientation-point-option)]
    (when auto-positioned?
      {:orientation (cond-> {:point orientation-point-option
                             :ui/label :string.option/orientation
                             :ui/element :ui.element/position}

                      (= current-orientation-point
                         :angle) (assoc :angle {:type :option.type/range
                                                :min 0
                                                :max 360
                                                :default 45
                                                :ui/label :string.option/angle})

                      (not= current-orientation-point
                            :angle) (assoc :offset-x {:type :option.type/range
                                                      :min -75
                                                      :max 75
                                                      :default 0
                                                      :ui/label :string.option/offset-x
                                                      :ui/step 0.1}
                                           :offset-y {:type :option.type/range
                                                      :min -75
                                                      :max 75
                                                      :default 0
                                                      :ui/label :string.option/offset-y
                                                      :ui/step 0.1}))
       :default-size {:type :option.type/range
                      :min 0.1
                      :max 90
                      :default default-size
                      :ui/label :string.option/default-size
                      :ui/step 0.1}
       :default-spacing {:type :option.type/range
                         :min -75
                         :max 75
                         :default default-spacing
                         :ui/label :string.option/default-spacing
                         :ui/step 0.1}
       :offset-y {:type :option.type/range
                  :min -75
                  :max 75
                  :default 0
                  :ui/label :string.option/offset-y
                  :ui/step 0.1}})))

(defmethod interface/options :heraldry/field [context]
  (let [path (:path context)
        root-field? (-> path drop-last last (= :coat-of-arms))
        subfield? (-> path last int?)
        semy-charge? (->> path (take-last 2) (= [:charge :field]))
        field-type (interface/get-raw-data (c/++ context :type))
        plain? (= field-type :heraldry.field.type/plain)
        counterchanged? (= field-type :heraldry.field.type/counterchanged)
        parent-type (interface/get-raw-data (-> context
                                                interface/parent
                                                interface/parent
                                                (c/++ :type)))]
    (cond-> (assoc (field.interface/options context)
                   :type type-option
                   :manual-blazon options/manual-blazon
                   :fess-group (fess-group-options context)
                   :pale-group (pale-group-options context)
                   :chevron-group (chevron-group-options context)
                   :bend-group (bend-group-options context))
      (not (or counterchanged?
               plain?)) (assoc :outline? options/plain-outline?-option)
      (or subfield?
          root-field?
          semy-charge?) (update-in [:type :choices] #(->> %
                                                          (filter (fn [[_ t]]
                                                                    (not= t :heraldry.field.type/counterchanged)))
                                                          vec))
      (and (isa? parent-type :heraldry/field)
           (not (or root-field?
                    semy-charge?))) (assoc :inherit-environment?
                                           {:type :option.type/boolean
                                            :default false
                                            :ui/label :string.option/inherit-environment?}))))
