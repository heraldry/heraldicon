(ns heraldry.frontend.state-hook)

(defn handle-db-changes [old-db new-db]
  (let [scope-path [:arms-form :render-options :scope]]
    (cond-> new-db
      (and (= (get-in new-db scope-path)
              :coat-of-arms)
           (not= (-> old-db :arms-form :helms)
                 (-> new-db :arms-form :helms))) (assoc-in scope-path :coat-of-arms-and-helm)
      (and (#{:coat-of-arms
              :coat-of-arms-and-helm} (get-in new-db scope-path))
           (not= (-> old-db :arms-form :ornaments)
                 (-> new-db :arms-form :ornaments))) (assoc-in scope-path :achievement))))
