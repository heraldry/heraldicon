(ns heraldry.frontend.ui.form.ordinary
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(rf/reg-event-db :remove-cottise
  (fn [db [_ path cottise]]
    (cond-> db
      (= cottise :cottise-1) (-> (update-in path dissoc :cottise-2)
                                 (assoc-in (conj path :cottise-1)
                                           (get-in db (conj path :cottise-2))))

      (= cottise :cottise-2) (update-in path dissoc :cottise-2)

      (= cottise :cottise-opposite-1) (-> (update-in path dissoc :cottise-opposite-2)
                                          (assoc-in (conj path :cottise-opposite-1)
                                                    (get-in db (conj path :cottise-opposite-2))))

      (= cottise :cottise-opposite-2) (update-in path dissoc :cottise-opposite-2))))

(defn form [path _]
  [:<>
   (for [option [:type
                 :variant
                 :line
                 :opposite-line
                 :extra-line
                 :escutcheon
                 :num-points
                 :angle
                 :origin
                 :direction-anchor
                 :anchor
                 :geometry
                 :fimbriation
                 :outline?
                 :manual-blazon]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/ordinary [path]
  (let [cottising-options @(rf/subscribe [:get-relevant-options (conj path :cottising)])
        cottise-1 @(rf/subscribe [:get-value (conj path :cottising :cottise-1)])
        cottise-2 @(rf/subscribe [:get-value (conj path :cottising :cottise-2)])
        cottise-opposite-1 @(rf/subscribe [:get-value (conj path :cottising :cottise-opposite-1)])
        cottise-opposite-2 @(rf/subscribe [:get-value (conj path :cottising :cottise-opposite-2)])
        cottise-1? (and (:cottise-1 cottising-options)
                        cottise-1)
        cottise-2? (and (:cottise-2 cottising-options)
                        cottise-1
                        cottise-2)
        cottise-opposite-1? (and (:cottise-opposite-1 cottising-options)
                                 cottise-opposite-1)
        cottise-opposite-2? (and (:cottise-opposite-2 cottising-options)
                                 cottise-opposite-1
                                 cottise-opposite-2)
        menu (cond-> []
               (and (:cottise-1 cottising-options)
                    (not cottise-1?))
               (conj {:title "Cottise 1"
                      :handler #(let [cottise-path (conj path :cottising :cottise-1)]
                                  (rf/dispatch-sync [:set cottise-path default/cottise])
                                  (state/dispatch-on-event % [:ui-component-node-select cottise-path {:open? true}]))})

               (and (:cottise-2 cottising-options)
                    cottise-1?
                    (not cottise-2?))
               (conj {:title "Cottise 2"
                      :handler #(let [cottise-path (conj path :cottising :cottise-2)]
                                  (rf/dispatch-sync [:set cottise-path default/cottise])
                                  (state/dispatch-on-event % [:ui-component-node-select cottise-path {:open? true}]))})

               (and (:cottise-opposite-1 cottising-options)
                    (not cottise-opposite-1?))
               (conj {:title "Cottise 1 (opposite)"
                      :handler #(let [cottise-path (conj path :cottising :cottise-opposite-1)]
                                  (rf/dispatch-sync [:set cottise-path default/cottise])
                                  (state/dispatch-on-event % [:ui-component-node-select cottise-path {:open? true}]))})

               (and (:cottise-opposite-2 cottising-options)
                    cottise-opposite-1?
                    (not cottise-opposite-2?))
               (conj {:title "Cottise 2 (opposite)"
                      :handler #(let [cottise-path (conj path :cottising :cottise-opposite-2)]
                                  (rf/dispatch-sync [:set cottise-path default/cottise])
                                  (state/dispatch-on-event % [:ui-component-node-select cottise-path {:open? true}]))}))]
    {:title (ordinary/title path {})
     :buttons [{:icon "fas fa-plus"
                :title "Add"
                :disabled? (empty? menu)
                :menu menu}]
     :nodes (cond-> [{:path (conj path :field)}]
              cottise-2? (conj {:path (conj path :cottising :cottise-2)
                                :buttons [{:icon "far fa-trash-alt"
                                           :tooltip "remove"
                                           :handler #(state/dispatch-on-event % [:remove-cottise (conj path :cottising) :cottise-2])}]})
              cottise-1? (conj {:path (conj path :cottising :cottise-1)
                                :buttons [{:icon "far fa-trash-alt"
                                           :tooltip "remove"
                                           :handler #(state/dispatch-on-event % [:remove-cottise (conj path :cottising) :cottise-1])}]})
              cottise-opposite-1? (conj {:path (conj path :cottising :cottise-opposite-1)
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :tooltip "remove"
                                                    :handler #(state/dispatch-on-event % [:remove-cottise (conj path :cottising) :cottise-opposite-1])}]})

              cottise-opposite-2? (conj {:path (conj path :cottising :cottise-opposite-2)
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :tooltip "remove"
                                                    :handler #(state/dispatch-on-event % [:remove-cottise (conj path :cottising) :cottise-opposite-2])}]}))}))

(defmethod interface/component-form-data :heraldry.component/ordinary [_path]
  {:form form})
