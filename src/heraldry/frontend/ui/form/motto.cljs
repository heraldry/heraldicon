(ns heraldry.frontend.ui.form.motto
  (:require [heraldry.frontend.ui.form.ribbon-general :as ribbon-general]
            [heraldry.frontend.ui.interface :as ui-interface]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option [:type
                 :origin
                 :geometry]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} "Tincture"]

   (for [option [:tincture-foreground
                 :tincture-background
                 :tincture-text]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} "Ribbon"]

   [ui-interface/form-element (conj path :ribbon-variant)]

   (when @(rf/subscribe [:get-value (conj path :ribbon-variant)])
     (let [ribbon-path (conj path :ribbon)]
       [:<>
        [ribbon-general/ribbon-form ribbon-path]
        [ribbon-general/ribbon-segments-form ribbon-path]]))])

(defmethod ui-interface/component-node-data :heraldry.component/motto [path]
  (let [num-mottos @(rf/subscribe [:get-list-size (-> path drop-last vec)])
        title (case @(rf/subscribe [:get-value (conj path :type)])
                :heraldry.motto.type/motto "Motto"
                :heraldry.motto.type/slogan "Slogan")]
    {:title (str (when (> num-mottos 1)
                   (str (inc (last path)) ". ")) title)}))

(defmethod ui-interface/component-form-data :heraldry.component/motto [_path]
  {:form form})

