(ns heraldry.coat-of-arms.ordinary.options
  (:require
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.type.base :as base]
   [heraldry.coat-of-arms.ordinary.type.bend :as bend]
   [heraldry.coat-of-arms.ordinary.type.bend-sinister :as bend-sinister]
   [heraldry.coat-of-arms.ordinary.type.chevron :as chevron]
   [heraldry.coat-of-arms.ordinary.type.chief :as chief]
   [heraldry.coat-of-arms.ordinary.type.cross :as cross]
   [heraldry.coat-of-arms.ordinary.type.fess :as fess]
   [heraldry.coat-of-arms.ordinary.type.gore :as gore]
   [heraldry.coat-of-arms.ordinary.type.label :as label]
   [heraldry.coat-of-arms.ordinary.type.pale :as pale]
   [heraldry.coat-of-arms.ordinary.type.pall :as pall]
   [heraldry.coat-of-arms.ordinary.type.pile :as pile]
   [heraldry.coat-of-arms.ordinary.type.point :as point]
   [heraldry.coat-of-arms.ordinary.type.quarter :as quarter]
   [heraldry.coat-of-arms.ordinary.type.saltire :as saltire]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinaries
  [pale/ordinary-type
   fess/ordinary-type
   chief/ordinary-type
   base/ordinary-type
   bend/ordinary-type
   bend-sinister/ordinary-type
   cross/ordinary-type
   saltire/ordinary-type
   chevron/ordinary-type
   pall/ordinary-type
   pile/ordinary-type
   gore/ordinary-type
   label/ordinary-type
   quarter/ordinary-type
   point/ordinary-type])

(def choices
  (->> ordinaries
       (map (fn [key]
              [(ordinary-interface/display-name key) key]))))

(def ordinary-map
  (util/choices->map choices))

(defn set-line-defaults [options]
  (-> options
      (options/override-if-exists [:fimbriation :alignment :default] :outside)))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label strings/type
        :form-type :ordinary-type-select}})


(defmethod interface/options-dispatch-fn :heraldry.component/ordinary [context]
  (interface/get-raw-data (c/++ context :type)))

(defmethod interface/component-options :heraldry.component/ordinary [context]
  (-> (interface/options context)
      (assoc :type type-option)
      (assoc :manual-blazon options/manual-blazon)
      (update :cottising (fn [cottising-options]
                           (when cottising-options
                             (cottising/options cottising-options
                                                (interface/get-raw-data (c/++ context :cottising))))))))
