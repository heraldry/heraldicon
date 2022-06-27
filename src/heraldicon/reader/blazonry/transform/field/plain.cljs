(ns heraldicon.reader.blazonry.transform.field.plain
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child]]))

(defmethod ast->hdn :plain [[_ & nodes]]
  (let [node (get-child #{:tincture
                          :COUNTERCHANGED} nodes)]
    (case (first node)
      :COUNTERCHANGED {:type :heraldry.field.type/counterchanged}
      {:type :heraldry.field.type/plain
       :tincture (ast->hdn node)})))
