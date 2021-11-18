(ns heraldry.state
  (:require
   [heraldry.component :as component]
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

(rf/reg-sub ::component-type
  (fn [[_ context] _]
    (rf/subscribe [:get (:path (c/++ context :type))]))

  (fn [raw-type [_ context]]
    (component/effective-type (:path context) raw-type)))

(rf/reg-sub ::options-for-component
  (fn [[_ context] _]
    (let [entity-type (rf/subscribe [:get (:path (c/++ context :type))])
          component-type (rf/subscribe [::component-type context])]
      (-> {:component-type component-type
           :entity-type entity-type}
          (merge (->> (interface/options-subscriptions
                       (-> context
                           (assoc :dispatch-value @component-type)
                           (assoc :entity-type @entity-type)))
                      (map (fn [relative-path]
                             [relative-path (rf/subscribe [:get
                                                           (:path (apply c/++ context relative-path))])]))
                      (into {}))))))

  (fn [{:keys [component-type entity-type] :as subscription-data} [_ context]]
    (when (or component-type entity-type)
      (let [subscription-context (-> context
                                     (assoc :dispatch-value component-type
                                            :entity-type entity-type
                                            :subscriptions {:base-path (:path context)
                                                            :data subscription-data}))]
        (interface/options subscription-context)))))

(rf/reg-sub ::option-path
  (fn [[_ {:keys [path] :as context}] _]
    (->> (range (count path))
         (mapv (fn [length]
                 (rf/subscribe [::component-type (c/-- context length)])))))

  (fn [component-types [_ {:keys [path]}]]
    (->> component-types
         (keep-indexed (fn [idx component-type]
                         (when (and component-type
                                    ;; some types are special in that they can't span their own
                                    ;; option set, but they still have a component node representation
                                    (not (#{:heraldry.component/cottise} component-type)))
                           (subvec path 0 (-> path count (- idx))))))
         first)))

(rf/reg-sub ::options
  (fn [[_ context] _]
    (let [option-path (rf/subscribe [::option-path context])
          component-options (rf/subscribe [::options-for-component (c/<< context :path @option-path)])]
      [option-path component-options]))

  (fn [[option-path component-options] [_ {:keys [path]}]]
    (let [relative-path (-> option-path count (drop path) vec)]
      (get-in component-options relative-path))))

(rf/reg-sub ::sanitized-data
  (fn [[_ {:keys [path] :as context}] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options context])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))
