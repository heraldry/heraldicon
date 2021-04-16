(ns heraldry.frontend.form.charge
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.charge :as frontend-charge]
            [heraldry.frontend.form.charge-map :as charge-map]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.form.geometry :as geometry]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [full-url-for-username]]
            [re-frame.core :as rf]))

(defn charge-type-choice [path key display-name & {:keys [current]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture :argent
                                   :components [{:type key
                                                 :geometry {:size 75}
                                                 :escutcheon (if (= key :heraldry.charge.type/escutcheon) :heater nil)
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture (if (= current key) :or :azure)}}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge path {:type key
                                                                                      :attitude nil
                                                                                      :facing nil
                                                                                      :data nil
                                                                                      :variant nil}])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn charge-type-selected-choice [charge display-name]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture :argent
                                   :components [{:type (:type charge)
                                                 :variant (:variant charge)
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture :or}}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-charge-type [path]
  (let [charge @(rf/subscribe [:get path])
        charge-type (:type charge)
        names (->> charge/choices
                   (map (comp vec reverse))
                   (into {}))
        title (util/combine " " [(or (get names charge-type)
                                     (-> charge :type util/translate-cap-first))
                                 (-> charge :attitude util/translate)
                                 (-> charge :facing util/translate)])]
    [:div.setting
     [:label "Type"]
     " "
     [element/submenu path "Select Charge" title {:min-width "22em"}
      (for [[display-name key] charge/choices]
        ^{:key key}
        [charge-type-choice path key display-name :current charge-type])
      (when (-> names (contains? charge-type) not)
        [charge-type-selected-choice charge title])
      (let [[status charges] (state/async-fetch-data
                              [:all-charges]
                              :all-charges
                              frontend-charge/fetch-charges)]
        [:div {:style {:padding "15px"}}
         (if (= status :done)
           [charge-map/charge-tree charges
            :refresh-action #(state/invalidate-cache [:all-charges] :all-charges)
            :render-variant (fn [node]
                              (let [charge-data (:data node)
                                    username (:username charge-data)]
                                [:div {:style {:display "inline-block"
                                               :white-space "normal"
                                               :vertical-align "top"
                                               :line-height "1.5em"}}
                                 [:div {:style {:display "inline-block"
                                                :vertical-align "top"}}
                                  [:a.clickable
                                   {:on-click #(state/dispatch-on-event
                                                %
                                                [:update-charge
                                                 path
                                                 (merge {:type (->> charge-data
                                                                    :type
                                                                    name
                                                                    (keyword "heraldry.charge.type"))
                                                         :variant {:id (:id charge-data)
                                                                   :version (:latest-version charge-data)}}
                                                        (select-keys charge-data
                                                                     [:attitude :facing]))])}
                                   (:name charge-data)]
                                  " by "
                                  [:a {:href (full-url-for-username username)
                                       :target "_blank"} username]]
                                 [charge-map/charge-properties charge-data]]))]
           [:div "loading..."])])]]))

(defn form [path & {:keys [parent-field form-for-field part-of-semy?]}]
  (let [charge @(rf/subscribe [:get path])
        charge-data (when-let [variant (:variant charge)]
                      (frontend-charge/fetch-charge-data variant))
        fixed-tincture (-> charge-data
                           :fixed-tincture
                           (or :none))
        supported-tinctures (-> attributes/tincture-modifier-map
                                keys
                                set
                                (conj :eyes-and-teeth)
                                (conj :secondary)
                                (conj :tertiary)
                                (conj :shadow)
                                (conj :highlight)
                                (set/intersection
                                 (-> charge-data
                                     :colours
                                     (->> (map second))
                                     set)))
        sorted-supported-tinctures (-> supported-tinctures
                                       (disj :eyes-and-teeth)
                                       (disj :shadow)
                                       (disj :highlight)
                                       sort
                                       vec)
        tinctures-set (-> charge
                          :tincture
                          (->> (filter (fn [[_ v]]
                                         (and (some? v)
                                              (not= v :none))))
                               (map first)
                               set)
                          (filter supported-tinctures)
                          (->> (map util/translate-cap-first)))
        tinctures-title (if (-> tinctures-set count pos?)
                          (util/combine ", " tinctures-set)
                          "Default")
        tinctures-title (if (-> tinctures-title count (> 30))
                          (str (subs tinctures-title 0 27) "...")
                          tinctures-title)
        title (s/join " " [(-> charge :type util/translate-cap-first)
                           (-> charge :attitude util/translate)])]
    [element/component path :charge title nil
     [:div.settings
      (when (:type charge)
        [form-for-charge-type path])
      (when (-> supported-tinctures
                count
                pos?)
        [:div.setting
         [:label "Tinctures"]
         " "
         [element/submenu (conj path :tincture) "Tinctures" tinctures-title {}
          (when sorted-supported-tinctures
            [:div.placeholders
             (when (get supported-tinctures :eyes-and-teeth)
               [element/checkbox
                (conj path :tincture :eyes-and-teeth)
                "White eyes and teeth"
                :on-change #(rf/dispatch [:set
                                          (conj path :tincture :eyes-and-teeth)
                                          (if % :argent nil)])])
             (when (get supported-tinctures :shadow)
               [element/range-input-with-checkbox
                (conj path :tincture :shadow)
                "Shadow"
                0
                1
                :step 0.01
                :default 1])
             (when (get supported-tinctures :highlight)
               [element/range-input-with-checkbox
                (conj path :tincture :highlight)
                "Highlight"
                0
                1
                :step 0.01
                :default 1])
             (for [t sorted-supported-tinctures]
               ^{:key t}
               [tincture/form
                (conj path :tincture t)
                :label (util/translate-cap-first t)])])]])
      (let [charge-options (charge-options/options charge
                                                   :part-of-semy? part-of-semy?)]
        [:<>
         (when (:origin charge-options)
           [position/form (conj path :origin)
            :title "Origin"
            :options (:origin charge-options)])
         (when (:anchor charge-options)
           [position/form (conj path :anchor)
            :title "Anchor"
            :options (:anchor charge-options)])
         (when (:geometry charge-options)
           [geometry/form (conj path :geometry)
            (:geometry charge-options)
            :current (:geometry charge)])
         (when (:escutcheon charge-options)
           [escutcheon/form (conj path :escutcheon) "Escutcheon"
            :choices (-> charge-options :escutcheon :choices)])
         (when (:fimbriation charge-options)
           [line/form-for-fimbriation (conj path :fimbriation) (:fimbriation charge-options)])])
      [element/select (conj path :hints :outline-mode) "Outline" [["Keep" :keep]
                                                                  ["Remove" :remove]
                                                                  ["Primary" :primary]
                                                                  ["Transparent" :transparent]]
       :default :keep]]
     (if (not= fixed-tincture :none)
       [:div {:style {:margin-bottom "0.5em"}}
        "Fixed tincture:" [:span.tag.fixed-tincture fixed-tincture]]
       [form-for-field (conj path :field) :parent-field parent-field])]))
