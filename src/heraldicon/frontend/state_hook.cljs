(ns heraldicon.frontend.state-hook)

(defn ^:export handle-db-changes [old-db new-db]
  ;; TODO: path shouldn't be hard-coded
  (let [base-path [:forms :heraldicon.entity/arms :data]
        scope-path (conj base-path :achievement :render-options :scope)
        helms-path (conj base-path :data :achievement :helms)
        ornaments-path (conj base-path :data :achievement :ornaments)]
    (cond-> new-db
      (and (= (get-in new-db scope-path)
              :coat-of-arms)
           (not= (get-in old-db helms-path)
                 (get-in new-db helms-path))) (assoc-in scope-path :coat-of-arms-and-helm)
      (and (#{:coat-of-arms
              :coat-of-arms-and-helm} (get-in new-db scope-path))
           (not= (get-in old-db ornaments-path)
                 (get-in new-db ornaments-path))) (assoc-in scope-path :achievement))))
