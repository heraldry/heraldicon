(ns heraldry.state
  (:require
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [re-frame.core :as rf]))

(rf/reg-sub :get
  (fn [db [_ path]]
    (if (map? path)
      (get-in db (:path path))
      (get-in db path))))

(rf/reg-sub :get-list-size
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (count value)))

(rf/reg-sub ::options
  (fn [db [_ {:keys [path] :as context}]]
    (let [[options relative-path] (or (->> (range (count path) 0 -1)
                                           (keep (fn [idx]
                                                   (let [option-path (subvec path 0 idx)
                                                         relative-path (subvec path idx)
                                                         options (interface/options
                                                                  (-> context
                                                                      (c/<< :path option-path)
                                                                      (assoc :direct-db db)))]
                                                     (when options
                                                       [options relative-path]))))
                                           first)
                                      [nil nil])]
      (get-in options relative-path))))

(rf/reg-sub ::sanitized-data
  (fn [[_ {:keys [path] :as context}] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options context])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))
