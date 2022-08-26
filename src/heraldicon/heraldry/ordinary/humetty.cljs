(ns heraldicon.heraldry.ordinary.humetty
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]))

(defn options [context]
  (let [humetty? (interface/get-raw-data (c/++ context :humetty?))]
    (cond-> {:humetty? {:type :option.type/boolean
                        :ui/label :string.option/humetty}
             :ui/label :string.option/humetty
             :ui/tooltip :string.tooltip/humetty-warning
             :ui/element :ui.element/humetty}
      humetty? (assoc :corner {:type :option.type/choice
                               :choices [[:string.option.corner-choice/round :round]
                                         [:string.option.corner-choice/sharp :sharp]
                                         [:string.option.corner-choice/bevel :bevel]]
                               :default :round
                               :ui/label :string.option/corner}
                      :distance {:type :option.type/range
                                 :min 1
                                 :max 45
                                 :default 5
                                 :ui/label :string.option/distance}))))

(defn coup [shape parent-shape {:keys [humetty? distance corner]}]
  (if humetty?
    (let [shape (if (vector? shape)
                  shape
                  [shape])
          shrunken-environment-shape (environment/shrink-shape parent-shape distance corner)
          shrink (fn shrink [path]
                   (environment/intersect-shapes path shrunken-environment-shape))]
      (if (vector? shape)
        (mapv shrink shape)
        (shrink shape)))
    shape))
