(ns heraldicon.frontend.state-hook)

(defn ^:export handle-db-changes [old-db new-db]
  (let [scope-path [:arms-form :data :achievement :render-options :scope]]
    (cond-> new-db
      (and (= (get-in new-db scope-path)
              :coat-of-arms)
           (not= (-> old-db :arms-form :data :achievement :helms)
                 (-> new-db :arms-form :data :achievement :helms))) (assoc-in scope-path :coat-of-arms-and-helm)
      (and (#{:coat-of-arms
              :coat-of-arms-and-helm} (get-in new-db scope-path))
           (not= (-> old-db :arms-form :data :achievement :ornaments)
                 (-> new-db :arms-form :data :achievement :ornaments))) (assoc-in scope-path :achievement))))
