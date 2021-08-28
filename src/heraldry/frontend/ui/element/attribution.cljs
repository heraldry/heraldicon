(ns heraldry.frontend.ui.element.attribution
  (:require [heraldry.attribution :as attribution]
            [heraldry.frontend.ui.element.select :as select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.macros :as macros]
            [heraldry.options :as options]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :attribution-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[attribution options] [_ _path]]
    (let [sanitized-attribution (options/sanitize attribution options)
          main-name (case (:nature sanitized-attribution)
                      :own-work "Own work"
                      :derivative "Derivative")
          license (:license attribution)
          changes [main-name
                   (if (= license :none)
                     "no license"
                     (attribution/license-display-name license (:license-version attribution)))]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(macros/reg-event-db :merge-attribution
  (fn [db [_ path data]]
    (update-in db path merge data)))

(defn attribution-submenu [path & {:keys [charge-presets?]}]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:attribution-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "29em"}
                                               :class "submenu-attribution"}
         [:<>
          (when charge-presets?
            [select/raw-select
             nil
             :none
             "Presets"
             [["Autofill for known sources" :none]
              ["WappenWiki" :wappenwiki]
              ["Wikimedia" :wikimedia]
              ["Wikimedia (Sodacan)" :wikimedia-sodacan]
              ["Encyclopedia Heraldica" :encyclopedia-heraldica]]
             :on-change (fn [value]
                          (case value
                            :wappenwiki (rf/dispatch [:merge-attribution
                                                      path
                                                      {:nature :derivative
                                                       :license :cc-attribution-non-commercial-share-alike
                                                       :license-version :v4
                                                       :source-license :cc-attribution-non-commercial-share-alike
                                                       :source-license-version :v3
                                                       :source-creator-name "WappenWiki"
                                                       :source-creator-link "http://wappenwiki.org"}])
                            :wikimedia (rf/dispatch [:merge-attribution
                                                     path
                                                     {:nature :derivative
                                                      :license :cc-attribution-share-alike
                                                      :license-version :v4
                                                      :source-license :cc-attribution-share-alike
                                                      :source-license-version :v3}])
                            :wikimedia-sodacan (rf/dispatch [:merge-attribution
                                                             path
                                                             {:nature :derivative
                                                              :license :cc-attribution-share-alike
                                                              :license-version :v4
                                                              :source-license :cc-attribution-share-alike
                                                              :source-license-version :v3
                                                              :source-creator-name "Sodacan"
                                                              :source-creator-link "https://commons.wikimedia.org/wiki/User:Sodacan"}])
                            :encyclopedia-heraldica (rf/dispatch [:set
                                                                  path
                                                                  {:nature :derivative
                                                                   :license :cc-attribution-share-alike
                                                                   :license-version :v4
                                                                   :source-license :cc-attribution-share-alike
                                                                   :source-license-version :v4
                                                                   :source-link "https://1drv.ms/u/s!Anj4BrtS8clIaQi3EIOCPpnfKQE?e=AkQ8lW"
                                                                   :source-creator-name "Encyclopedia Heraldica"
                                                                   :source-creator-link "https://1drv.ms/u/s!Anj4BrtS8clIaQi3EIOCPpnfKQE?e=AkQ8lW"}])
                            nil))])

          (for [option [:nature
                        :license
                        :license-version]]
            ^{:key option} [interface/form-element (conj path option)])

          [:div {:style {:height "1.5em"}}]

          (for [option [:source-license
                        :source-license-version
                        :source-name
                        :source-link
                        :source-creator-name
                        :source-creator-link]]
            ^{:key option} [interface/form-element (conj path option)])]]]])))

(defmethod interface/form-element :attribution [path]
  [attribution-submenu path
   :charge-presets? (-> path drop-last last #{:charge-form
                                              :charge-data})])
