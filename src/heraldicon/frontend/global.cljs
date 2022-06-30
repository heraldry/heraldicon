(ns heraldicon.frontend.global
  (:require
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(macros/reg-event-db :set
  (fn [db [_ context value]]
    (if (vector? context)
      (assoc-in db context value)
      (assoc-in db (:path context) value))))

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
