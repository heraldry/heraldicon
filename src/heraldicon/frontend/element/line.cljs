(ns heraldicon.frontend.element.line
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: likely can be further improved by reading out the various description strings
;; in separate subscriptions
(defn- submenu-link-name [options line]
  (let [changes [(-> line :type line/line-map)
                 (when (some #(options/changed? % line options)
                             [:eccentricity :spacing :offset :base-line])
                   :string.submenu-summary/adjusted)
                 (when (some #(options/changed? % line options)
                             [:width :height])
                   :string.submenu-summary/resized)
                 (when (:mirrored? line)
                   :string.submenu-summary/mirrored)
                 (when (:flipped? line)
                   :string.submenu-summary/flipped)
                 (when (and (-> line :fimbriation :mode)
                            (-> line :fimbriation :mode (not= :none)))
                   :string.submenu-summary/fimbriated)]]
    (string/upper-case-first (string/combine ", " changes))))

(defmethod element/element :ui.element/line [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label
         [tr link-name]
         {:style {:width "26em"}
          :class "submenu-line"}
         (element/elements
          context
          [:type
           :size-reference
           :width
           :height
           :eccentricity
           :spacing
           :offset
           :base-line
           :corner-damping-radius
           :corner-damping-mode
           :mirrored?
           :flipped?
           :fimbriation])]]])))
