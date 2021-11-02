(ns heraldry.state
  (:require
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(rf/reg-sub :get
  (fn [db [_ path]]
    (get-in db path)))

(rf/reg-sub :get-list-size
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (count value)))

(rf/reg-sub :get-options
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ path]]
    (interface/component-options path data)))

(rf/reg-sub :get-relevant-options
  (fn [_ [_ path]]
    ;; TODO: can this be done by feeding the subscriptions in again?
    ;; probably is more efficient, but the previous attempt didn't refresh the
    ;; subscription properly when the options changed (e.g. switching to "arc" in a charge-group)
    (->> (range (count path) 0 -1)
         (keep (fn [idx]
                 (let [option-path (subvec path 0 idx)
                       relative-path (subvec path idx)
                       options @(rf/subscribe [:get-options option-path])]
                   (when-let [relevant-options (get-in options relative-path)]
                     relevant-options))))
         first)))
