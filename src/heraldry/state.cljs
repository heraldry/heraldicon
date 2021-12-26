(ns heraldry.state
  (:require
   [heraldry.component :as component]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

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
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [raw-type [_ path]]
    (component/effective-type path raw-type)))

(rf/reg-sub-raw ::options
  (fn [_app-db [_ path]]
    (reaction
     (when (seq path)
       (let [component-type @(rf/subscribe [::component-type path])
             entity-type @(rf/subscribe [:get (conj path :type)])]
         ;; TODO: entity-type could be a line style, which here doesn't count as
         ;; component on its own; probably would be cleaner to deal with that special
         ;; case (and others?) in a less magical way than checking for a namespace
         (if (or component-type
                 (some-> entity-type namespace))
           (let [context {:path path
                          :dispatch-value component-type
                          :entity-type entity-type}]
             (-> context
                 (assoc :subscriptions {:base-path path
                                        :data (->> (interface/options-subscriptions context)
                                                   (map (fn [relative-path]
                                                          [relative-path
                                                           @(rf/subscribe [:get (vec (concat path relative-path))])]))
                                                   (into {}))})
                 interface/options))
           (-> @(rf/subscribe [::options (pop path)])
               (get (last path)))))))))

(rf/reg-sub ::sanitized-data
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options path])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))
