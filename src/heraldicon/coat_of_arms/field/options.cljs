(ns heraldicon.coat-of-arms.field.options
  (:require
   [heraldicon.coat-of-arms.field.interface :as field.interface]
   [heraldicon.coat-of-arms.field.type.barry :as barry]
   [heraldicon.coat-of-arms.field.type.bendy :as bendy]
   [heraldicon.coat-of-arms.field.type.bendy-sinister :as bendy-sinister]
   [heraldicon.coat-of-arms.field.type.chequy :as chequy]
   [heraldicon.coat-of-arms.field.type.chevronny :as chevronny]
   [heraldicon.coat-of-arms.field.type.counterchanged :as counterchanged]
   [heraldicon.coat-of-arms.field.type.fretty :as fretty]
   [heraldicon.coat-of-arms.field.type.gyronny :as gyronny]
   [heraldicon.coat-of-arms.field.type.gyronny-n :as gyronny-n]
   [heraldicon.coat-of-arms.field.type.lozengy :as lozengy]
   [heraldicon.coat-of-arms.field.type.masony :as masony]
   [heraldicon.coat-of-arms.field.type.paly :as paly]
   [heraldicon.coat-of-arms.field.type.papellony :as papellony]
   [heraldicon.coat-of-arms.field.type.per-bend :as per-bend]
   [heraldicon.coat-of-arms.field.type.per-bend-sinister
    :as per-bend-sinister]
   [heraldicon.coat-of-arms.field.type.per-chevron :as per-chevron]
   [heraldicon.coat-of-arms.field.type.per-fess :as per-fess]
   [heraldicon.coat-of-arms.field.type.per-pale :as per-pale]
   [heraldicon.coat-of-arms.field.type.per-pile :as per-pile]
   [heraldicon.coat-of-arms.field.type.per-saltire :as per-saltire]
   [heraldicon.coat-of-arms.field.type.plain :as plain]
   [heraldicon.coat-of-arms.field.type.potenty :as potenty]
   [heraldicon.coat-of-arms.field.type.quartered :as quartered]
   [heraldicon.coat-of-arms.field.type.quarterly :as quarterly]
   [heraldicon.coat-of-arms.field.type.tierced-per-fess :as tierced-per-fess]
   [heraldicon.coat-of-arms.field.type.tierced-per-pale :as tierced-per-pale]
   [heraldicon.coat-of-arms.field.type.tierced-per-pall :as tierced-per-pall]
   [heraldicon.coat-of-arms.field.type.vairy :as vairy]
   [heraldicon.context :as c]
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
