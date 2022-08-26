(ns heraldicon.heraldry.ordinary.voided
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]))

(defn options [context]
  (let [voided? (interface/get-raw-data (c/++ context :voided?))]
    (cond-> {:voided? {:type :option.type/boolean
                       :ui/label :string.charge.attribute/voided}
             :ui/label :string.charge.attribute/voided
             :ui/tooltip :string.tooltip/humetty-warning
             :ui/element :ui.element/voided}
      voided? (assoc :corner {:type :option.type/choice
                              :choices [[:string.option.corner-choice/round :round]
                                        [:string.option.corner-choice/sharp :sharp]
                                        [:string.option.corner-choice/bevel :bevel]]
                              :default :sharp
                              :ui/label :string.option/corner}
                     :thickness {:type :option.type/range
                                 :min 1
                                 :max 45
                                 :default 10
                                 :ui/label :string.option/thickness}))))

(defn void [shape parent-shape {:keys [voided? thickness corner]}]
  (if voided?
    (let [exact-shape (environment/intersect-shapes (first shape) parent-shape)
          inner-shape (environment/shrink-shape exact-shape thickness corner)]
      (conj shape inner-shape))
    shape))
