(ns heraldry.coat-of-arms.counterchange
  (:require [heraldry.coat-of-arms.division.core :as division]))

(defn counterchange-field [field {:keys [division]}]
  (-> field
      (dissoc :content)
      (assoc :division {:type (:type division)
                        :line (:line division)
                        :layout (:layout division)
                        :fields (-> (division/default-fields division)
                                    (assoc-in [0 :content :tincture] (get-in division [:fields 1 :content :tincture]))
                                    (assoc-in [1 :content :tincture] (get-in division [:fields 0 :content :tincture])))})))

(defn counterchangable? [field parent]
  (and (:counterchanged? field)
       (division/counterchangable? (-> parent :division))))
