(ns heraldry.frontend.ui.element.value-mode-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn value-mode-select [path & {:keys [display-fn disabled?]}]
  (let [value @(rf/subscribe [:get-value path])
        {:keys [inherited
                default
                type
                choices]} @(rf/subscribe [:get-relevant-options path])
        display-fn (or display-fn
                       (when (= type :choice)
                         (util/choices->map choices))
                       identity)
        menu (cond-> []
               (and inherited
                    default) (conj {:title (str "Default (" (display-fn default) ")")
                                    :icon "fas fa-redo"
                                    :handler #(state/dispatch-on-event % [:set path default])})
               (or inherited
                   default) (conj {:title (str (if inherited
                                                 "Inherited"
                                                 "Auto")
                                               " (" (display-fn (or inherited default)) ")")
                                   :icon (if value
                                           "far fa-square"
                                           "far fa-check-square")
                                   :handler #(state/dispatch-on-event % [:set path nil])}))
        menu (cond-> menu
               (seq menu) (conj {:title "Manual"
                                 :icon (if value
                                         "far fa-check-square"
                                         "far fa-square")
                                 :handler #(state/dispatch-on-event % [:set path (or value
                                                                                     inherited
                                                                                     default)])}))]

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
