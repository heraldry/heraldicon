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
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

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
   gyronny-n/field-type
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
              [(field.interface/display-name key) key]))))

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
    [:anchor :point]
    [:origin :point]
    [:orientation :point]
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
