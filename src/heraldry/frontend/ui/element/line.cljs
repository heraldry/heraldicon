(ns heraldry.frontend.ui.element.line
  (:require
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

;; TODO: likely can be further improved by reading out the various description strings
;; in separate subscriptions
(defn submenu-link-name[options line]
  (let [changes [(-> line :type line/line-map)
                 (when (some #(options/changed? % line options)
                             [:eccentricity :spacing :offset :base-line])
                   (string "adjusted"))
                 (when (some #(options/changed? % line options)
                             [:width :height])
                   (string "resized"))
                 (when (:mirrored? line)
                   (string "mirrored"))
                 (when (:flipped? line)
                   (string "flipped"))
                 (when (and (-> line :fimbriation :mode)
                            (-> line :fimbriation :mode (not= :none)))
                   (string "fimbriated"))]]
    (-> (util/combine ", " changes)
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
         (ui-interface/form-elements
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

(defmethod ui-interface/form-element :line [context]
  [line-submenu context])
