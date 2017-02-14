// Generated on 2016-11-01 using generator-angular 0.15.1
'use strict';

var gulp = require('gulp');
var $ = require('gulp-load-plugins')();
var openURL = require("open");
var lazypipe = require('lazypipe');
var rimraf = require('rimraf');
var wiredep = require('wiredep').stream;
var runSequence = require('run-sequence');

var yeoman = {
  app: require('./bower.json').appPath || 'app',
  dist: 'dist'
};

var paths = {
  scripts: [yeoman.app + '/scripts/**/*.js'],
  buildScriptsDest: yeoman.app + '/build-scripts',
  buildScripts : [yeoman.app + '/build-scripts/**/*.js'],
  serverScripts: ['server/**/*.js'],
  styles: [yeoman.app + '/sass/**/*.scss'],
  test: ['test/spec/**/*.js'],
  testRequire: [
    yeoman.app + '/bower_components/angular/angular.js',
    yeoman.app + '/bower_components/angular-mocks/angular-mocks.js',
    yeoman.app + '/bower_components/angular-resource/angular-resource.js',
    yeoman.app + '/bower_components/angular-cookies/angular-cookies.js',
    yeoman.app + '/bower_components/angular-sanitize/angular-sanitize.js',
    yeoman.app + '/bower_components/angular-route/angular-route.js',
    'test/mock/**/*.js',
    'test/spec/**/*.js'
  ],
  karma: 'karma.conf.js',
  views: {
    main: yeoman.app + '/index.html',
    files: [yeoman.app + '/views/**/*.html']
  }
};

////////////////////////
// Reusable pipelines //
////////////////////////

var lintScripts = lazypipe()
  .pipe($.jshint, '.jshintrc')
  .pipe($.jshint.reporter, 'jshint-stylish');

var styles = lazypipe()
  .pipe($.sass, {
    outputStyle: 'expanded',
    precision: 10
  })
  .pipe($.autoprefixer, 'last 1 version')
  .pipe(gulp.dest, 'app/styles');

var es6ClientScript = lazypipe()
  .pipe($.babel, {
    presets: ['es2015']
  })
  .pipe(gulp.dest, paths.buildScriptsDest);

var es6ServerScript = lazypipe()
  .pipe($.babel, {
    presets: ['es2015']
  })
  .pipe(gulp.dest, 'build-server');

///////////
// Tasks //
///////////

gulp.task('styles', function () {
  return gulp.src(paths.styles)
    .pipe(styles());
});

gulp.task('lint:clientScripts', function () {
  return gulp.src(paths.scripts)
    .pipe(lintScripts());
});

gulp.task('lint:serverScripts', function () {
  return gulp.src(paths.serverScripts)
    .pipe(lintScripts());
});

gulp.task('start:client', ['start:server', 'styles'], function () {
  openURL("http://localhost:9000","Chrome");
});

gulp.task('start:server', function() {
  $.nodemon({
    script: 'build-server/app.js',
    ignore: ["app","dist","upload","node","node-v6.9.1","build-server"]
  });
});

gulp.task('watch', function () {
  $.livereload.listen();
  $.watch(paths.styles)
    .pipe($.plumber())
    .pipe(styles())
    .pipe($.livereload());

  $.watch(paths.views.files)
    .pipe($.plumber())
    .pipe($.livereload());

  $.watch(paths.scripts)
    .pipe($.plumber())
    .pipe(lintScripts())
    .pipe(es6ClientScript())
    .pipe($.livereload());

  $.watch(paths.serverScripts)
    .pipe($.plumber())
    .pipe(lintScripts())
    .pipe(es6ServerScript());

  $.watch(paths.test)
    .pipe($.plumber())
    .pipe($.livereload());

  gulp.watch('bower.json', ['bower']);
});

gulp.task('serve', function (cb) {
  runSequence(
    ['lint:clientScripts'],
    ['lint:serverScripts'],
    ['start:client'],
	  ['bower'],
    'watch', cb);
});

//TODO: test should be changed that karma config is wrong
gulp.task('start:server:test', function() {
  $.nodemon({
    script: 'build-server/app.js',
    ignore: ["app","dist","upload","node","node-v6.9.1","build-server"]
  });
});

gulp.task('test', ['start:server:test'], function () {
  var testToFiles = paths.testRequire.concat(paths.scripts, paths.test);
  return gulp.src(testToFiles)
    .pipe($.karma({
      configFile: paths.karma,
      action: 'watch'
    }));
});

// inject bower components
gulp.task('bower', function () {
  return gulp.src(paths.views.main)
    .pipe(wiredep({
      directory: yeoman.app + '/bower_components',
      ignorePath: '..',
      exclude: ['bower_components/mermaid/dist/mermaid.slim.js']
    }))
  .pipe(gulp.dest(yeoman.app));
});

///////////
// Build //
///////////

gulp.task('clean:dist', function (cb) {
  rimraf('./dist', cb);
});

gulp.task('clean:server', function(cb) {
  rimraf('./build-server', cb);
});

gulp.task('clean:client', function(cb) {
  rimraf('./app/build-scripts', cb);
});

gulp.task('es6:frontend', () => {
  return gulp.src(paths.scripts)
    .pipe(es6ClientScript());
});

gulp.task('es6:server', () => {
  return gulp.src(paths.serverScripts)
    .pipe(es6ServerScript());
});

gulp.task('client:build', ['html', 'styles', 'es6:frontend', 'es6:server'], function () {
  var jsFilter = $.filter('**/*.js');
  var cssFilter = $.filter('**/*.css');

  return gulp.src(paths.views.main)
    .pipe($.useref({
      transformPath: function(filePath) {
        if(filePath.indexOf("/app/bower_components") === -1) {
          return filePath.replace('/bower_components', '/app/bower_components')
        }else{
          return filePath;
        }
      }
    }))
    .pipe(jsFilter)
    .pipe($.ngAnnotate())
    .pipe($.uglify())
    .pipe(jsFilter.restore())
    .pipe(cssFilter)
    .pipe($.minifyCss({cache: true}))
    .pipe(cssFilter.restore())
    .pipe($.rev())
    .pipe($.revReplace())
    .pipe(gulp.dest(yeoman.dist));
});

gulp.task('client:rename', ['client:build'], function(){
  return gulp.src(yeoman.dist + "/index*.html")
    .pipe($.rename("index.html"))
    .pipe(gulp.dest(yeoman.dist))
});

gulp.task('html', function () {
  return gulp.src(yeoman.app + '/views/**/*')
    .pipe(gulp.dest(yeoman.dist + '/views'));
});

gulp.task('images', function () {
  return gulp.src(yeoman.app + '/images/**/*')
    .pipe($.cache($.imagemin({
        optimizationLevel: 5,
        progressive: true,
        interlaced: true
    })))
    .pipe(gulp.dest(yeoman.dist + '/images'));
});

gulp.task('favicon', function(){
  return gulp.src(yeoman.app + '/favicon.ico')
    .pipe(gulp.dest(yeoman.dist));
});

gulp.task('pageNotFound', function(){
  return gulp.src(yeoman.app + '/404.html')
    .pipe(gulp.dest(yeoman.dist));
});

gulp.task('copy:extras', function () {
  return gulp.src(yeoman.app + '/*/.*', { dot: true })
    .pipe(gulp.dest(yeoman.dist));
});

gulp.task('copy:fonts', function () {
  return gulp.src(yeoman.app + '/fonts/**/*')
    .pipe(gulp.dest(yeoman.dist + '/fonts'));
});

gulp.task('build', ['clean:dist', 'clean:server', 'clean:client'], function () {
  runSequence(['images', 'favicon', 'copy:extras', 'copy:fonts', 'client:rename', 'pageNotFound']);
});

gulp.task('default', ['build']);
