(ns heraldicon.reader.blazonry.transform
  (:require
   [heraldicon.reader.blazonry.transform.amount] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.charge] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.charge-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.cottising] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.partition.field] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.plain] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.layout] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.line] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinal] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.ordinary-group] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child]]
   [heraldicon.reader.blazonry.transform.tincture] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.tincture-modifier :refer [add-tincture-modifiers]]))

(defn- semy [charge nodes]
  (let [layout (some-> (get-child #{:layout
                                    :horizontal-layout
                                    :vertical-layout} nodes)
                       ast->hdn)]
    (cond-> {:type :heraldry/semy
             :charge charge}
      layout (assoc :layout layout))))

(defmethod ast->hdn :semy [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field} nodes))
        charge (-> #{:charge-without-fimbriation}
                   (get-child nodes)
                   ast->hdn
                   (assoc :field field)
                   (add-tincture-modifiers nodes)
                   (add-fimbriation nodes))]
    [(semy charge nodes)]))

(defn transform [ast]
  (ast->hdn ast))
