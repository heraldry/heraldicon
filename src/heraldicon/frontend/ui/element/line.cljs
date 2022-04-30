(ns heraldicon.frontend.ui.element.line
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.translation.string :as string]
   [heraldicon.util :as util]))

;; TODO: likely can be further improved by reading out the various description strings
;; in separate subscriptions
(defn submenu-link-name [options line]
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
    (-> (string/combine ", " changes)
        util/upper-case-first)))

(defn line-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label
         [tr link-name]
         {:style {:width "26em"}
          :class "submenu-line"}
         (ui.interface/form-elements
          context
          [:type
           :eccentricity
           :height
           :width
           :spacing
           :offset
           :base-line
           :corner-dampening-radius
           :corner-dampening-mode
           :mirrored?
           :flipped?
           :fimbriation])]]])))

(defmethod ui.interface/form-element :line [context]
  [line-submenu context])
