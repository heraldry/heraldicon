(ns heraldry.frontend.ui.form.motto
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.form.ribbon-general :as ribbon-general]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :motto-name
  (fn [[_ path] _]
    (rf/subscribe [:get (drop-last path)]))

  (fn [elements [_ path]]
    ;; TODO: fix numbering/naming
    (let [idx (last path)
          mottos (keep-indexed (fn [idx element]
                                 (when (-> element :type (= :heraldry.motto.type/motto))
                                   idx)) elements)
          slogans (keep-indexed (fn [idx element]
                                  (when (-> element :type (= :heraldry.motto.type/slogan))
                                    idx)) elements)
          relevant-elements (if (some (set [idx]) mottos)
                              mottos
                              slogans)]
      (util/str-tr
       (when (-> relevant-elements count (> 1))
         (str (inc (util/index-of idx relevant-elements)) ". "))
       (if (= relevant-elements mottos)
         strings/motto
         strings/slogan)))))

(defn form [path]
  [:<>
   (for [option [:type
                 :origin
                 :geometry]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr strings/tincture]]

   (for [option [:tincture-foreground
                 :tincture-background
                 :tincture-text]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr strings/ribbon]]

   [ui-interface/form-element (conj path :ribbon-variant)]

   (when @(rf/subscribe [:get-value (conj path :ribbon-variant)])
     (let [ribbon-path (conj path :ribbon)]
       [:<>
        [ribbon-general/ribbon-form ribbon-path]
        [ribbon-general/ribbon-segments-form ribbon-path]]))])

(defmethod ui-interface/component-node-data :heraldry.component/motto [{:keys [path]}]
  {:title @(rf/subscribe [:motto-name path])})

(defmethod ui-interface/component-form-data :heraldry.component/motto [_context]
  {:form form})
