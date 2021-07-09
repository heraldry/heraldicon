(ns heraldry.frontend.ui.form.ordinary
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as interface]
            [taoensso.timbre :as log]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option [:type
                 :variant
                 :line
                 :opposite-line
                 :escutcheon
                 :num-points
                 :angle
                 :origin
                 :direction-anchor
                 :anchor
                 :geometry
                 :fimbriation
                 :cottising
                 :outline?]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/ordinary [path component-data component-options]
  (let [cottising-options (:cottising component-options)
        {:keys [cottise-1
                cottise-2
                cottise-opposite-1
                cottise-opposite-2]} (:cottising component-data)
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
                      :handler #(state/dispatch-on-event
                                 % [:set (conj path :cottising :cottise-1) default/cottise])})

               (and (:cottise-2 cottising-options)
                    cottise-1
                    (not cottise-2?))
               (conj {:title "Cottise 2"
                      :handler #(state/dispatch-on-event
                                 % [:set (conj path :cottising :cottise-2) default/cottise])})

               (and (:cottise-opposite-1 cottising-options)
                    (not cottise-opposite-1?))
               (conj {:title "Cottise 1 (opposite)"
                      :handler #(state/dispatch-on-event
                                 % [:set (conj path :cottising :cottise-opposite-1) default/cottise])})

               (and (:cottise-opposite-2 cottising-options)
                    cottise-opposite-1
                    (not cottise-opposite-2?))
               (conj {:title "Cottise 2 (opposite)"
                      :handler #(state/dispatch-on-event
                                 % [:set (conj path :cottising :cottise-opposite-2) default/cottise])}))]
    {:title (ordinary/title component-data)
     :buttons [{:icon "fas fa-plus"
                :title "Add"
                :disabled? (empty? menu)
                :menu menu}]
     :nodes (cond-> [{:path (conj path :field)}]
              cottise-2? (conj {:path (conj path :cottising :cottise-2)
                                :buttons [{:icon "far fa-trash-alt"
                                           :tooltip "remove"
                                           :handler #(state/dispatch-on-event
                                                      % [:set (conj path :cottising :cottise-2) nil])}]})
              cottise-1? (conj {:path (conj path :cottising :cottise-1)
                                :buttons [{:icon "far fa-trash-alt"
                                           :tooltip "remove"
                                           :handler #(do
                                                       (rf/dispatch [:set (conj path :cottising :cottise-1) cottise-2])
                                                       (state/dispatch-on-event
                                                        % [:set (conj path :cottising :cottise-2) nil]))}]})
              cottise-opposite-1? (conj {:path (conj path :cottising :cottise-opposite-1)
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :tooltip "remove"
                                                    :handler #(do
                                                                (rf/dispatch [:set (conj path :cottising :cottise-opposite-1) cottise-opposite-2])
                                                                (state/dispatch-on-event
                                                                 % [:set (conj path :cottising :cottise-opposite-2) nil]))}]})

              cottise-opposite-2? (conj {:path (conj path :cottising :cottise-opposite-2)
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :tooltip "remove"
                                                    :handler #(state/dispatch-on-event
                                                               % [:set (conj path :cottising :cottise-opposite-2) nil])}]}))}))

(defmethod interface/component-form-data :heraldry.component/ordinary [_component-data]
  {:form form})
