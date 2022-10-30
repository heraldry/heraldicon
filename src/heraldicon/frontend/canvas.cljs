(ns heraldicon.frontend.canvas
  (:require
   [cljs.core.async.interop :refer-macros [<p!]]
   [com.wsscode.async.async-cljs :refer [go-catch <?]]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.util.async :as util.async]
   [reagent.dom.server :as r-server]))

(defn draw-svg [svg-data target-width target-height]
  (go-catch
   (let [svg-data-str (r-server/render-to-static-markup
                       [:svg {:viewBox (str "0 0 " target-width " " target-height)
                              :version "1.1"
                              :xmlns "http://www.w3.org/2000/svg"
                              :width target-width
                              :height target-height}
                        svg-data])
         img (js/Image.)
         canvas (js/OffscreenCanvas. target-width target-height)
         blob (js/Blob. (clj->js [svg-data-str]) (clj->js {:type "image/svg+xml"}))
         url (js/window.URL.createObjectURL blob)]
     (<p! (util.async/promise-from-callback
           (fn [callback]
             (set! img.onload (fn []
                                (let [context (doto (.getContext canvas "2d")
                                                (.drawImage img 0 0))
                                      image-data (.getImageData context 0 0 target-width target-height)]
                                  (callback nil image-data))))
             (set! img.src url)))))))

(defn- painted-bounding-box [image]
  (let [pixels (.-data image)
        width (.-width image)
        alphas (take-nth 4 (drop 3 pixels))
        painted-coords (keep-indexed (fn [index alpha]
                                       (when (pos? alpha)
                                         (v/Vector. (mod index width)
                                                    (quot index width))))
                                     alphas)]
    (-> (bb/from-points painted-coords)
        (update :max-x inc)
        (update :max-y inc))))

(def ^:private max-depth 3)

(defn svg-bounding-box [svg-data base-bounding-box & {:keys [depth]
                                                      :or {depth 0}}]
  (go-catch
   (let [target-width 256
         target-height 256
         width (bb/width base-bounding-box)
         height (bb/height base-bounding-box)
         top-left (bb/top-left base-bounding-box)
         scale (min (/ target-width width)
                    (/ target-height height))
         shifted-svg-data [:g {:transform (str "scale(" scale "," scale ")"
                                               "translate(" (- (:x top-left)) "," (- (:y top-left)) ")")}
                           svg-data]
         image (<? (draw-svg shifted-svg-data target-width target-height))
         bounding-box (painted-bounding-box image)
         bounding-box (if (<= depth max-depth)
                        (<? (svg-bounding-box shifted-svg-data bounding-box :depth (inc depth)))
                        bounding-box)
         bounding-box (-> bounding-box
                          (bb/scale (/ 1 scale))
                          (bb/translate top-left))]
     bounding-box)))

(comment

  (go-catch
   (js/console.log :test (<? (svg-bounding-box [:rect {:x 10.34
                                                       :y 10.3
                                                       :width 8.12
                                                       :height 6.12
                                                       :fill "red"}]
                                               (bb/from-vector-and-size v/zero 256 256)))))

;;
  )
