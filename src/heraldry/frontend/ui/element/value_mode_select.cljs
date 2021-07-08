(ns heraldry.frontend.ui.element.value-mode-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]))

(defn value-mode-select [path & {:keys [value inherited default
                                        display-fn disabled?]}]
  (let [menu (cond-> []
               (and inherited
                    default) (conj {:title (str "Default (" (if display-fn
                                                              (display-fn default)
                                                              default) ")")
                                    :icon "fas fa-redo"
                                    :handler #(state/dispatch-on-event % [:set path default])})
               (or inherited
                   default) (conj {:title (str (if inherited
                                                 "Inherited"
                                                 "Auto")
                                               " (" (let [derived-value (or inherited default)]
                                                      (if display-fn
                                                        (display-fn derived-value)
                                                        derived-value)) ")")
                                   :icon (if value
                                           "far fa-square"
                                           "far fa-check-square")
                                   :handler #(state/dispatch-on-event % [:set path nil])}))
        menu (cond-> menu
               (seq menu) (conj {:title "Manual"
                                 :icon (if value
                                         "far fa-check-square"
                                         "far fa-square")
                                 :handler #(state/dispatch-on-event % [:set path value])}))]

    [:<>
     (when (seq menu)
       [:div {:style {:display "inline-block"
                      :margin-left "0.5em"
                      :position "absolute"}}
        [hover-menu/hover-menu
         path
         "Mode"
         menu
         [:i.ui-icon {:class "fas fa-cog"}]
         :disabled? disabled?]])]))
