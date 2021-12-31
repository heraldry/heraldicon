(ns heraldry.coat-of-arms.field.options
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.type.barry :as barry]
   [heraldry.coat-of-arms.field.type.bendy :as bendy]
   [heraldry.coat-of-arms.field.type.bendy-sinister :as bendy-sinister]
   [heraldry.coat-of-arms.field.type.chequy :as chequy]
   [heraldry.coat-of-arms.field.type.chevronny :as chevronny]
   [heraldry.coat-of-arms.field.type.counterchanged :as counterchanged]
   [heraldry.coat-of-arms.field.type.fretty :as fretty]
   [heraldry.coat-of-arms.field.type.gyronny :as gyronny]
   [heraldry.coat-of-arms.field.type.lozengy :as lozengy]
   [heraldry.coat-of-arms.field.type.masony :as masony]
   [heraldry.coat-of-arms.field.type.paly :as paly]
   [heraldry.coat-of-arms.field.type.papellony :as papellony]
   [heraldry.coat-of-arms.field.type.per-bend :as per-bend]
   [heraldry.coat-of-arms.field.type.per-bend-sinister
    :as per-bend-sinister]
   [heraldry.coat-of-arms.field.type.per-chevron :as per-chevron]
   [heraldry.coat-of-arms.field.type.per-fess :as per-fess]
   [heraldry.coat-of-arms.field.type.per-pale :as per-pale]
   [heraldry.coat-of-arms.field.type.per-pile :as per-pile]
   [heraldry.coat-of-arms.field.type.per-saltire :as per-saltire]
   [heraldry.coat-of-arms.field.type.plain :as plain]
   [heraldry.coat-of-arms.field.type.potenty :as potenty]
   [heraldry.coat-of-arms.field.type.quartered :as quartered]
   [heraldry.coat-of-arms.field.type.quarterly :as quarterly]
   [heraldry.coat-of-arms.field.type.tierced-per-fess :as tierced-per-fess]
   [heraldry.coat-of-arms.field.type.tierced-per-pale :as tierced-per-pale]
   [heraldry.coat-of-arms.field.type.tierced-per-pall :as tierced-per-pall]
   [heraldry.coat-of-arms.field.type.vairy :as vairy]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def fields
  [plain/field-type
   counterchanged/field-type
   per-pale/field-type
   per-fess/field-type
   per-bend/field-type
   per-bend-sinister/field-type
   per-chevron/field-type
   per-saltire/field-type
   quartered/field-type
   quarterly/field-type
   gyronny/field-type
   tierced-per-pale/field-type
   tierced-per-fess/field-type
   tierced-per-pall/field-type
   per-pile/field-type
   paly/field-type
   barry/field-type
   bendy/field-type
   bendy-sinister/field-type
   chevronny/field-type
   chequy/field-type
   lozengy/field-type
   vairy/field-type
   potenty/field-type
   papellony/field-type
   masony/field-type
   fretty/field-type])

(def choices
  (->> fields
       (map (fn [key]
              [(field-interface/display-name key) key]))))

(def field-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label :string.option/partition
        :form-type :field-type-select}})

(defmethod interface/options-subscriptions :heraldry.component/field [_context]
  #{[:type]
    [:line]
    [:line :type]
    [:line :fimbriation]
    [:line :fimbriation :mode]
    [:opposite-line :type]
    [:opposite-line :fimbriation :mode]
    [:extra-line :type]
    [:extra-line :fimbriation :mode]
    [:origin :point]
    [:direction-anchor :point]
    [:anchor :point]
    [:geometry :size-mode]
    [:tincture]})

(defmethod interface/options :heraldry.component/field [context]
  (let [path (:path context)
        root-field? (-> path drop-last last (= :coat-of-arms))
        subfield? (-> path last int?)
        semy-charge? (->> path (take-last 2) (= [:charge :field]))
        field-type (interface/get-raw-data (c/++ context :type))
        plain? (= field-type :heraldry.field.type/plain)
        counterchanged? (= field-type :heraldry.field.type/counterchanged)
        ref? (= field-type :heraldry.field.type/ref)]
    (cond-> {:manual-blazon options/manual-blazon}
      (not (or counterchanged?
               plain?
               ref?)) (assoc :outline? options/plain-outline?-option)
      (not ref?) (-> (merge (interface/options (assoc context :dispatch-value field-type)))
                     (assoc :type type-option))
      (and (not ref?)
           (or subfield?
               root-field?
               semy-charge?)) (update-in [:type :choices] #(->> %
                                                                (filter (fn [[_ t]]
                                                                          (not= t :heraldry.field.type/counterchanged)))
                                                                vec))
      (not (or root-field?
               semy-charge?
               counterchanged?
               ref?)) (assoc :inherit-environment?
                             {:type :boolean
                              :default false
                              :ui {:label :string.option/inherit-environment?}}))))
