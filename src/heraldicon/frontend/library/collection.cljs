(ns heraldicon.frontend.library.collection
  (:require
   ["copy-to-clipboard" :as copy-to-clipboard]
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as entity.attribution]
   [heraldicon.entity.id :as id]
   [heraldicon.font :as font]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.element.collection-select :as collection-select]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]
   [heraldicon.frontend.ui.form.entity.collection.element :as collection.element]
   [heraldicon.frontend.ui.shared :as shared]
   [heraldicon.frontend.user :as user]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.render.core :as render]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def form-db-path
  [:collection-form])

(def ^:private saved-data-db-path
  [:saved-collection-data])

(def ^:private list-db-path
  [:collection-list])

(defn- save-collection-clicked []
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (try
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            response (<? (api.request/call :save-collection payload user-data))
            collection-id (:id response)]
        (rf/dispatch-sync [:set (conj form-db-path :id) collection-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (state/invalidate-cache-without-current form-db-path [collection-id nil])
        (state/invalidate-cache-without-current form-db-path [collection-id 0])
        (rf/dispatch-sync [:set list-db-path nil])
        (state/invalidate-cache list-db-path (:user-id user-data))
        (rf/dispatch-sync [:set-form-message form-db-path
                           (string/str-tr :string.user.message/collection-saved " " (:version response))])
        (reife/push-state :view-collection-by-id {:id (id/for-url collection-id)}))
      (catch :default e
        (log/error "save-form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])))))

(defn- render-add-arms [x y size]
  (let [r (* size 0.4)
        bar-width (* size 0.5)
        bar-height (* size 0.1)]
    [:g {:transform (str "translate(" x "," y ")")}
     [:circle
      {:r r
       :style {:fill "#ccc"
               :stroke-width 0.1
               :stroke "#000"}}]
     [:rect
      {:x (/ bar-width -2)
       :y (/ bar-height -2)
       :width bar-width
       :height bar-height
       :style {:fill "#fff"}}]
     [:rect
      {:x (/ bar-height -2)
       :y (/ bar-width -2)
       :width bar-height
       :height bar-width
       :style {:fill "#fff"}}]]))

(defn- selected-element-index []
  (let [selected-node-path @(rf/subscribe [:collection-library-highlighted-element])
        index (last selected-node-path)]
    (when (int? index)
      (if (< index @(rf/subscribe [:get-list-size (conj form-db-path :data :elements)]))
        index
        ;; index not valid anymore
        (do
          (collection.element/highlight-element nil)
          nil)))))

(defn- arms-highlight [path x y width height]
  (if @(rf/subscribe [:collection-library-highlighted? path])
    [:rect {:x (- x (/ width 2) 7)
            :y (- y (/ height 2) 7)
            :rx 10
            :width (+ width 14)
            :height (+ height 14)
            :fill "#33f8"}]
    [:<>]))

(defn- render-arms [x y size path & {:keys [font font-size]
                                     :or {font-size 12}}]
  (let [data @(rf/subscribe [:get path])
        {arms-id :id
         version :version} (:reference data)
        [_status arms-data] (when arms-id
                              (state/async-fetch-data
                               [:arms-references arms-id version]
                               [arms-id version]
                               #(api/fetch-arms arms-id version nil)))
        collection-render-options (interface/get-raw-data {:path (conj form-db-path :data :render-options)})
        {:keys [result
                environment]} (render/coat-of-arms
                               (-> shared/coa-select-option-context
                                   (c/<< :path [:context :coat-of-arms])
                                   (c/<< :render-options (merge-with
                                                          (fn [old new]
                                                            (if (nil? new)
                                                              old
                                                              new))
                                                          (:render-options shared/coa-select-option-context)
                                                          (-> arms-data :data :achievement :render-options)
                                                          collection-render-options))
                                   (c/<< :coat-of-arms
                                         (if-let [coat-of-arms (-> arms-data :data :achievement :coat-of-arms)]
                                           coat-of-arms
                                           default/coat-of-arms)))
                               size)
        {:keys [width height]} environment]
    [:g
     [arms-highlight path x y width height]
     [:g {:transform (str "translate(" (- x (/ width 2)) "," (- y (/ height 2)) ")")}
      result
      [:text {:x (/ width 2)
              :y (+ height 10 font-size)
              :text-anchor "middle"
              :style {:font-family font
                      :font-size font-size}}
       (:name data)]]]))

(defn- on-arms-click [event index]
  (state/dispatch-on-event event [:ui-component-node-select (conj form-db-path :data :elements index)]))

(defn- render-collection [& {:keys [allow-adding?]}]
  (let [font (some-> (interface/get-sanitized-data {:path (conj form-db-path :data :font)})
                     font/css-string)
        num-columns (interface/get-sanitized-data {:path (conj form-db-path :data :num-columns)})
        num-elements (interface/get-list-size {:path (conj form-db-path :data :elements)})
        name @(rf/subscribe [:get (conj form-db-path :name)])
        num-rows (inc (quot num-elements
                            num-columns))
        margin 10
        arms-width 100
        roll-width (+ (* num-columns
                         arms-width)
                      (* (inc num-columns)
                         margin))
        arms-height (* 1.6 arms-width)
        roll-height (+ 60
                       (* num-rows
                          arms-height)
                       (* (inc num-rows)
                          margin))]
    [:svg {:id "svg"
           :style {:width "100%"}
           :viewBox (str "0 0 " roll-width " " roll-height)
           :preserveAspectRatio "xMidYMin slice"}
     [:g
      [:text {:x (/ roll-width 2)
              :y 50
              :text-anchor "middle"
              :style {:font-family font
                      :font-size 50}}
       name]]
     [:g {:transform "translate(0,60)"}
      (into [:<>]
            (map (fn [idx]
                   (let [x (mod idx num-columns)
                         y (quot idx num-columns)]
                     ^{:key idx}
                     [:g {:on-click #(on-arms-click % idx)
                          :style {:cursor "pointer"}}
                      [render-arms
                       (+ margin
                          (* x (+ arms-width
                                  margin))
                          (+ (/ arms-width 2)))
                       (+ margin
                          (* y (+ arms-height
                                  margin))
                          (+ (/ arms-height 2)))
                       arms-width
                       (conj form-db-path :data :elements idx)
                       :font font]])))
            (range num-elements))

      (when allow-adding?
        (let [x (mod num-elements num-columns)
              y (quot num-elements num-columns)]
          ^{:key num-elements}
          [:g {:on-click #(state/dispatch-on-event
                           % [:add-element {:path (conj form-db-path :data :elements)}
                              default/collection-element])
               :style {:cursor "pointer"}}
           [render-add-arms
            (+ margin
               (* x (+ arms-width
                       margin))
               (+ (/ arms-width 2)))
            (+ margin
               (* y (+ arms-height
                       margin))
               (+ (/ arms-height 2)))
            arms-width]]))]]))

(defn- render-arms-preview []
  (when-let [selected-element-index (selected-element-index)]
    (let [arms-reference @(rf/subscribe [:get (conj form-db-path :data :elements selected-element-index :reference)])
          {arms-id :id
           version :version} arms-reference
          [status arms-data] (when arms-id
                               (state/async-fetch-data
                                [:arms-references arms-id version]
                                [arms-id version]
                                #(api/fetch-arms arms-id version nil)))
          collection-render-options (interface/get-raw-data {:path (conj form-db-path :data :render-options)})]
      (when (or (not arms-id)
                (= status :done))
        (let [{:keys [result
                      environment]} (render/coat-of-arms
                                     (-> shared/coa-select-option-context
                                         (c/<< :path [:context :coat-of-arms])
                                         (c/<< :render-options (merge-with
                                                                (fn [old new]
                                                                  (if (nil? new)
                                                                    old
                                                                    new))
                                                                (:render-options shared/coa-select-option-context)
                                                                (-> arms-data :data :achievement :render-options)
                                                                collection-render-options))
                                         (c/<< :coat-of-arms
                                               (if-let [coat-of-arms (-> arms-data :data :achievement :coat-of-arms)]
                                                 coat-of-arms
                                                 default/coat-of-arms)))
                                     100)
              {:keys [width height]} environment]
          [:<>
           (when arms-id
             [:div.attribution
              [attribution/for-arms {:path [:context :arms]
                                     :arms arms-data}]])
           [:svg {:id "svg"
                  :style {:width "100%"}
                  :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
                  :preserveAspectRatio "xMidYMin meet"}
            [:g {:transform "translate(10,10) scale(5,5)"}
             result]]])))))

(defn- copy-to-new-clicked []
  (let [collection-data @(rf/subscribe [:get form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (state/set-async-fetch-data
     form-db-path
     :new
     (dissoc collection-data
             :id
             :version
             :latest-version
             :username
             :user-id
             :created-at
             :first-version-created-at
             :name))
    (rf/dispatch-sync [:set-form-message form-db-path :string.user.message/created-unsaved-copy])
    (reife/push-state :create-collection)))

(defn- share-button-clicked []
  (let [url (entity.attribution/full-url-for-collection {:path form-db-path})]
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (if (copy-to-clipboard url)
      (rf/dispatch [:set-form-message form-db-path :string.user.message/copied-url-for-sharing])
      (rf/dispatch [:set-form-error form-db-path :string.user.message/copy-to-clipboard-failed]))))

(defn- button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        collection-id @(rf/subscribe [:get (conj form-db-path :id)])
        collection-username @(rf/subscribe [:get (conj form-db-path :username)])
        public? (= @(rf/subscribe [:get (conj form-db-path :access)])
                   :public)
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= (update-in @(rf/subscribe [:get form-db-path]) [:data :achievement]
                                          dissoc :render-options)
                               (update-in @(rf/subscribe [:get saved-data-db-path]) [:data :achievement]
                                          dissoc :render-options))
        saved? collection-id
        owned-by-me? (= (:username user-data) collection-username)
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))
        can-share? (and public?
                        saved?
                        (not unsaved-changes?))]
    [:<>
     (when form-message
       [:div.success-message [tr form-message]])
     (when error-message
       [:div.error-message error-message])

     [:div.buttons {:style {:display "flex"}}
      [:div {:style {:flex "auto"}}]
      [:button.button {:style {:flex "initial"
                               :color "#777"}
                       :class (when-not can-share? "disabled")
                       :title (when-not can-share? (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))
                       :on-click (when can-share?
                                   share-button-clicked)}
       [:i.fas.fa-share-alt]]
      [hover-menu/hover-menu
       {:path [:arms-form-action-menu]}
       :string.button/actions
       [{:title :string.button/copy-to-new
         :icon "fas fa-clone"
         :handler copy-to-new-clicked
         :disabled? (not can-copy?)
         :tooltip (when-not can-copy?
                    (tr :string.user.message/need-to-be-logged-in-and-arms-must-be-saved))}
        {:title :string.button/share
         :icon "fas fa-share-alt"
         :handler share-button-clicked
         :disabled? (not can-share?)
         :tooltip (when-not can-share?
                    (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))}]
       [:button.button {:style {:flex "initial"
                                :color "#777"
                                :margin-left "10px"}}
        [:i.fas.fa-ellipsis-h]]
       :require-click? true]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-collection-clicked
                                           #(js/alert (tr :string.user.message/need-to-be-logged-in-and-own-the-collection.)))
                               :style {:flex "initial"
                                       :margin-left "10px"}}
       [tr :string.button/save]]]]))

(defn- collection-form []
  (rf/dispatch [:set-title-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-collection])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [render-collection :allow-adding? true]
   [:<>
    [ui/selected-component]
    [button-row]
    [render-arms-preview]]
   [:<>
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj form-db-path :data :render-options)
                        (conj form-db-path :data)]]]))

(defn- collection-display [collection-id version]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path collection-id])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path collection-id]))
  (let [[status _collection-data] (state/async-fetch-data
                                   form-db-path
                                   [collection-id version]
                                   #(api/fetch-collection collection-id version saved-data-db-path))]
    (when (= status :done)
      [collection-form])))

(defn link-to-collection [collection]
  (let [collection-id (-> collection
                          :id
                          id/for-url)]
    [:a {:href (reife/href :view-collection-by-id {:id collection-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (:name collection)]))

(defn view-list []
  (rf/dispatch [:set-title :string.entity/collections])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [tr :string.text.collection-library/create-and-view-collections]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-collection))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [collection-select/list-collections link-to-collection]]])

(defn create [_match]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path nil]))
  (let [[status collection-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go default/collection-entity))]
    (when (= status :done)
      (if collection-data
        [collection-form]
        [not-found/not-found]))))

(defn view-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        collection-id (str "collection:" id)]
    (if (or (nil? version)
            (util/integer-string? version))
      [collection-display collection-id version]
      [not-found/not-found])))
