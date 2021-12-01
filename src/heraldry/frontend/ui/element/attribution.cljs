(ns heraldry.frontend.ui.element.attribution
  (:require
   [heraldry.attribution :as attribution]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.select :as select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :attribution-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/sanitized-data (c/++ context :nature)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :license)])
     (rf/subscribe [:heraldry.state/sanitized-data (c/++ context :license-version)])])

  (fn [[nature license license-version] [_ _path]]
    (let [main-name (attribution/nature-map nature)
          changes [main-name
                   (if (= license :none)
                     (string "no license")
                     (attribution/license-display-name license license-version))]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(macros/reg-event-db :merge-attribution
  (fn [db [_ path data]]
    (update-in db path merge data)))

(defn attribution-submenu [{:keys [path] :as context} & {:keys [charge-presets?]}]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:attribution-submenu-link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "31em"}
                                                       :class "submenu-attribution"}
         [:<>
          (when charge-presets?
            [select/raw-select
             nil
             :none
             "Presets"
             [[(string "Autofill for known sources") :none]
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

          (ui-interface/form-elements
           context
           [:nature
            :license
            :license-version])

          [:div {:style {:height "1.5em"}}]

          (ui-interface/form-elements
           context
           [:source-license
            :source-license-version
            :source-name
            :source-link
            :source-creator-name
            :source-creator-link])]]]])))

(defmethod ui-interface/form-element :attribution [context]
  [attribution-submenu context
   :charge-presets? (-> context :path drop-last last #{:charge-form
                                                       :charge-data})])
