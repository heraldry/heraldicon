(ns heraldry.frontend.ui.element.value-mode-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn value-mode-select [path & {:keys [display-fn disabled?]}]
  (let [current-value @(rf/subscribe [:get-value path])
        {:keys [inherited
                default
                type
                choices]} @(rf/subscribe [:get-relevant-options path])
        display-fn (or display-fn
                       (when (= type :choice)
                         (util/choices->map choices))
                       identity)
        effective-value (->> [current-value
                              inherited
                              default]
                             (keep (fn [v]
                                     (when-not (nil? v)
                                       v)))
                             first)
        menu (cond-> []
               (and (some? inherited)
                    (some? default)) (conj {:title (str "Default (" (display-fn default) ")")
                                            :icon "fas fa-redo"
                                            :handler #(state/dispatch-on-event % [:set path default])})
               (or (some? inherited)
                   (some? default)) (conj {:title (str (if (some? inherited)
                                                         "Inherited"
                                                         "Auto")
                                                       " (" (display-fn (or inherited default)) ")")
                                           :icon (if (some? current-value)
                                                   "far fa-square"
                                                   "far fa-check-square")
                                           :handler #(state/dispatch-on-event % [:set path nil])}))
        menu (cond-> menu
               (seq menu) (conj {:title "Manual"
                                 :icon (if (some? current-value)
                                         "far fa-check-square"
                                         "far fa-square")
                                 :handler #(state/dispatch-on-event % [:set path effective-value])}))]

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
