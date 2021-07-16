(ns heraldry.frontend.ui.element.value-mode-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn value-mode-select [path & {:keys [display-fn disabled? on-change default-option]}]
  (let [current-value @(rf/subscribe [:get-value path])
        handler-for-value (fn [new-value]
                            (fn [event]
                              (if on-change
                                (do
                                  (on-change new-value)
                                  (.stopPropagation event))
                                (state/dispatch-on-event event [:set path new-value]))))
        {:keys [inherited
                default
                type
                choices]} (or @(rf/subscribe [:get-relevant-options path])
                              default-option)
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
                                            :handler (handler-for-value default)})
               (or (some? inherited)
                   (some? default)) (conj {:title (str (if (some? inherited)
                                                         "Inherited"
                                                         "Auto")
                                                       " (" (display-fn (or inherited default)) ")")
                                           :icon (if (some? current-value)
                                                   "far fa-square"
                                                   "far fa-check-square")
                                           :handler (handler-for-value nil)}))
        menu (cond-> menu
               (seq menu) (conj {:title "Manual"
                                 :icon (if (some? current-value)
                                         "far fa-check-square"
                                         "far fa-square")
                                 :handler (handler-for-value effective-value)}))]

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
