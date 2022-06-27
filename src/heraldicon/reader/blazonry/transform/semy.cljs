(ns heraldicon.reader.blazonry.transform.semy
  (:require
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first]]
   [heraldicon.reader.blazonry.transform.tincture-modifier :refer [add-tincture-modifiers]]))

(defn- semy [charge layout]
  (cond-> {:type :heraldry/semy
           :charge charge}
    layout (assoc :layout layout)))

(defmethod ast->hdn :semy [[_ & nodes]]
  (let [field (transform-first #{:field} nodes)
        charge (-> (transform-first #{:charge-without-fimbriation} nodes)
                   (assoc :field field)
                   (add-tincture-modifiers nodes)
                   (add-fimbriation nodes))
        layout (transform-first #{:layout
                                  :horizontal-layout
                                  :vertical-layout} nodes)]
    [(semy charge layout)]))
