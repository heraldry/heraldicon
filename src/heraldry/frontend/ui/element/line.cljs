(ns heraldry.frontend.ui.element.line
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :line-title
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:fimbriation-title (conj path :fimbriation)])])

  (fn [[line fimbriation-title] [_ _path]]
    (let [effective-data (options/sanitize line (line/options line))]
      (-> (util/combine
           ", "
           [(or (-> effective-data :type line/line-map)
                "Inherit")
            (when (and (-> effective-data :offset)
                       (-> effective-data :offset zero? not))
              (str "shifted"))
            (when (and (-> effective-data :spacing)
                       (-> effective-data :spacing zero? not))
              (str "spaced"))
            (when (and (-> effective-data :rotation)
                       (-> effective-data :rotation zero? not))
              (str "rotated"))
            (when (not= fimbriation-title "None")
              (str "fimbriated " fimbriation-title))])
          s/lower-case
          util/upper-case-first))))

(defn line-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:line-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
         (for [option [:type
                       :eccentricity
                       :height
                       :width
                       :spacing
                       :offset
                       :base-line
                       :mirrored?
                       :flipped?
                       :fimbriation]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :line [path]
  [line-submenu path])
