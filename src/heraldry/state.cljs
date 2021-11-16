(ns heraldry.state
  (:require
   [heraldry.context :as c]
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

(rf/reg-sub :get-relevant-options
  (fn [_ [_ path]]
    ;; TODO: can this be done by feeding the subscriptions in again?
    ;; probably is more efficient, but the previous attempt didn't refresh the
    ;; subscription properly when the options changed (e.g. switching to "arc" in a charge-group)
    (->> (range (count path) 0 -1)
         (keep (fn [idx]
                 (let [option-path (subvec path 0 idx)
                       relative-path (subvec path idx)
                       options (interface/options {:path option-path})]
                   (when-let [relevant-options (get-in options relative-path)]
                     relevant-options))))
         first)))

(rf/reg-sub ::get-from-context
  (fn [db [_ context]]
    (get-in db (:path context))))

(rf/reg-sub ::component-type-info
  (fn [[_ context] _]
    (rf/subscribe [::get-from-context (c/++ context :type)]))

  (fn [raw-type [_ context]]
    {:component-type (interface/raw-effective-component-type (:path context) raw-type)
     :entity-type raw-type}))

(rf/reg-sub ::options
  (fn [[_ context] _]
    (let [component-type-info (rf/subscribe [::component-type-info context])]
      (-> {:component-type-info component-type-info}
          (merge (->> (interface/options-subscriptions
                       (assoc context :component-type-info @component-type-info))
                      (map (fn [relative-path]
                             [relative-path (rf/subscribe [:get (into (:path context)
                                                                      relative-path)])]))
                      (into {}))))))

  (fn [{:keys [component-type-info] :as subscription-data} [_ context]]
    (let [subscription-context (-> context
                                   (assoc :component-type-info component-type-info)
                                   (assoc :path [:subscriptions])
                                   (assoc :subscriptions subscription-data))]
      (js/console.log :hi subscription-context)
      (interface/options subscription-context))))
